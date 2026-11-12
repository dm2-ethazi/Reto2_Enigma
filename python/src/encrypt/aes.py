"""Cifrado y descifrado AES-256-GCM por bloques con el contenedor R2AE v1."""

import os
import secrets
import string
import struct
from dataclasses import dataclass, field
from pathlib import Path

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

from ..errors import ValidationError
from ..i18n import tr
from ..utils.files import input_file, new_destination, staged_output

MAGIC = b"R2AE"
VERSION = 1
ITERATIONS = 200_000
SALT_SIZE = 16
NONCE_SIZE = 12
TAG_SIZE = 16
# Orden de red: firma, versión, iteraciones, sal y nonce; 37 bytes de cabecera.
HEADER = struct.Struct(">4sBI16s12s")
HEADER_SIZE = HEADER.size
OVERHEAD = HEADER_SIZE + TAG_SIZE
CHUNK_SIZE = 1024 * 1024
PASSWORD_ALPHABET = string.ascii_uppercase + string.ascii_lowercase + string.digits
# Límite de un único mensaje GCM: 2**39 - 256 bits.
MAX_PLAINTEXT_SIZE = (1 << 36) - 32


@dataclass(frozen=True)
class EncryptionResult:
    """Resultado de un cifrado publicado correctamente.

    Attributes:
        path: Ruta del archivo R2AE generado.
        password: Contraseña aleatoria de 16 caracteres, excluida de repr para
            evitar su exposición accidental en registros de depuración.
    """

    path: Path
    password: str = field(repr=False)


def generate_password() -> str:
    """Genera 16 letras o dígitos ASCII usando el generador seguro secrets."""
    return "".join(secrets.choice(PASSWORD_ALPHABET) for _ in range(16))


def _derive_key(password: str, salt: bytes) -> bytes:
    """Deriva una clave AES de 32 bytes con PBKDF2-HMAC-SHA256 y la sal indicada."""
    return PBKDF2HMAC(
        algorithm=hashes.SHA256(), length=32, salt=salt, iterations=ITERATIONS
    ).derive(password.encode("ascii"))


def encrypt_file(
    source: str | Path, destination: str | Path | None = None
) -> EncryptionResult:
    """Cifra un archivo con AES-256-GCM y publica un contenedor R2AE v1.

    Args:
        source: Ruta del archivo regular de entrada, que se conserva intacto.
        destination: Destino nuevo; por defecto añade .enc a la ruta original.

    Returns:
        Ruta del contenedor y contraseña generada, tras completar la publicación.

    Raises:
        ValidationError: Si el origen no es regular o supera el límite AES-GCM.
        FileExistsError: Si el destino ya existe, incluso al publicar.
        OSError: Si falla la lectura, la escritura o la publicación del archivo.

    Lee bloques de 1 MiB y escribe temporales de hasta 20 MiB. La cabecera se
    autentica como AAD y la etiqueta GCM se añade al final. La contraseña no se
    guarda en el contenedor y debe conservarse fuera de él.
    """
    source = input_file(source)
    destination = new_destination(
        destination if destination is not None else f"{source}.enc"
    )
    with source.open("rb") as original:
        if os.fstat(original.fileno()).st_size > MAX_PLAINTEXT_SIZE:
            raise ValidationError(
                tr("El archivo supera el límite de un mensaje AES-GCM.")
            )
        password = generate_password()
        salt = secrets.token_bytes(SALT_SIZE)
        nonce = secrets.token_bytes(NONCE_SIZE)
        header = HEADER.pack(MAGIC, VERSION, ITERATIONS, salt, nonce)
        encryptor = Cipher(
            algorithms.AES(_derive_key(password, salt)), modes.GCM(nonce)
        ).encryptor()
        # La cabecera viaja en claro, pero GCM detecta cualquier modificación.
        encryptor.authenticate_additional_data(header)
        with staged_output(destination) as output:
            output.write(header)
            total = 0
            while block := original.read(CHUNK_SIZE):
                total += len(block)
                # El origen puede crecer después de consultar su tamaño inicial.
                if total > MAX_PLAINTEXT_SIZE:
                    raise ValidationError(
                        tr("El archivo supera el límite de un mensaje AES-GCM.")
                    )
                output.write(encryptor.update(block))
            output.write(encryptor.finalize())
            output.write(encryptor.tag)
    return EncryptionResult(destination, password)


def decrypt_file(
    source: str | Path, password: str, destination: str | Path | None = None
) -> Path:
    """Descifra R2AE y publica el resultado solo tras autenticarlo.

    Args:
        source: Archivo R2AE v1 cifrado.
        password: Contraseña original de 16 letras o números ASCII.
        destination: Destino nuevo; por defecto elimina .enc o añade .decrypted.

    Returns:
        Ruta del archivo descifrado y autenticado.

    Raises:
        ValidationError: Si la contraseña, el formato, el tamaño o la
            autenticación son incorrectos, o el archivo cambia al leerlo.
        FileExistsError: Si el destino ya existe.
        OSError: Si falla el acceso o la publicación de los archivos.

    Los temporales contienen texto en claro pendiente de autenticación. Un error
    impide su publicación y provoca su limpieza mediante staged_output.
    """
    if len(password) != 16 or any(c not in PASSWORD_ALPHABET for c in password):
        raise ValidationError(
            tr("La contraseña AES debe contener 16 letras o números ASCII.")
        )
    source = input_file(source)
    default = (
        source.with_suffix("")
        if source.suffix.lower() == ".enc"
        else Path(f"{source}.decrypted")
    )
    destination = new_destination(destination if destination is not None else default)
    with source.open("rb") as original:
        size = os.fstat(original.fileno()).st_size
        if not OVERHEAD <= size <= MAX_PLAINTEXT_SIZE + OVERHEAD:
            raise ValidationError(tr("Tamaño de archivo R2AE no válido."))
        header = original.read(HEADER_SIZE)
        if len(header) != HEADER_SIZE:
            raise ValidationError(tr("Cabecera R2AE incompleta."))
        magic, version, iterations, salt, nonce = HEADER.unpack(header)
        # Rechazar parámetros ajenos al formato antes de ejecutar la KDF costosa.
        if (magic, version, iterations) != (MAGIC, VERSION, ITERATIONS):
            raise ValidationError(
                tr("Cabecera R2AE no válida o versión no compatible.")
            )
        original.seek(-TAG_SIZE, os.SEEK_END)
        tag = original.read(TAG_SIZE)
        if len(tag) != TAG_SIZE:
            raise ValidationError(tr("Etiqueta AES-GCM incompleta."))
        decryptor = Cipher(
            algorithms.AES(_derive_key(password, salt)), modes.GCM(nonce, tag)
        ).decryptor()
        decryptor.authenticate_additional_data(header)
        original.seek(HEADER_SIZE)
        with staged_output(destination) as output:
            remaining = size - OVERHEAD
            while remaining:
                block = original.read(min(CHUNK_SIZE, remaining))
                if not block:
                    raise ValidationError(
                        tr("Archivo R2AE truncado durante la lectura.")
                    )
                remaining -= len(block)
                output.write(decryptor.update(block))
            if original.read(TAG_SIZE) != tag or original.read(1):
                raise ValidationError(tr("El archivo R2AE cambió durante la lectura."))
            try:
                # update produce bytes antes de autenticar: aún siguen en partes.
                # Salir de staged_output sin error permite publicar el resultado.
                output.write(decryptor.finalize())
            except InvalidTag as error:
                raise ValidationError(
                    tr(
                        "No se pudo autenticar el archivo: contraseña incorrecta "
                        "o contenido/cabecera AAD alterados."
                    )
                ) from error
    return destination
