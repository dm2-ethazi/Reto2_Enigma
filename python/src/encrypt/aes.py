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
from ..utils.files import input_file, new_destination, staged_output

MAGIC = b"R2AE"
VERSION = 1
ITERATIONS = 200_000
SALT_SIZE = 16
NONCE_SIZE = 12
TAG_SIZE = 16
HEADER = struct.Struct(">4sBI16s12s")
HEADER_SIZE = HEADER.size
OVERHEAD = HEADER_SIZE + TAG_SIZE
CHUNK_SIZE = 1024 * 1024
PASSWORD_ALPHABET = string.ascii_uppercase + string.ascii_lowercase + string.digits
# Límite de un único mensaje GCM: 2**39 - 256 bits.
MAX_PLAINTEXT_SIZE = (1 << 36) - 32


@dataclass(frozen=True)
class EncryptionResult:
    path: Path
    password: str = field(repr=False)


def generate_password() -> str:
    return "".join(secrets.choice(PASSWORD_ALPHABET) for _ in range(16))


def _derive_key(password: str, salt: bytes) -> bytes:
    return PBKDF2HMAC(
        algorithm=hashes.SHA256(), length=32, salt=salt, iterations=ITERATIONS
    ).derive(password.encode("ascii"))


def encrypt_file(
    source: str | Path, destination: str | Path | None = None
) -> EncryptionResult:
    """Cifra cualquier archivo a partes de hasta 20 MiB y publica un único .enc."""
    source = input_file(source)
    destination = new_destination(
        destination if destination is not None else f"{source}.enc"
    )
    with source.open("rb") as original:
        if os.fstat(original.fileno()).st_size > MAX_PLAINTEXT_SIZE:
            raise ValidationError("El archivo supera el límite de un mensaje AES-GCM.")
        password = generate_password()
        salt = secrets.token_bytes(SALT_SIZE)
        nonce = secrets.token_bytes(NONCE_SIZE)
        header = HEADER.pack(MAGIC, VERSION, ITERATIONS, salt, nonce)
        encryptor = Cipher(
            algorithms.AES(_derive_key(password, salt)), modes.GCM(nonce)
        ).encryptor()
        encryptor.authenticate_additional_data(header)
        with staged_output(destination) as output:
            output.write(header)
            total = 0
            while block := original.read(CHUNK_SIZE):
                total += len(block)
                if total > MAX_PLAINTEXT_SIZE:
                    raise ValidationError(
                        "El archivo supera el límite de un mensaje AES-GCM."
                    )
                output.write(encryptor.update(block))
            output.write(encryptor.finalize())
            output.write(encryptor.tag)
    return EncryptionResult(destination, password)


def decrypt_file(
    source: str | Path, password: str, destination: str | Path | None = None
) -> Path:
    """Descifra R2AE a partes; publica solo tras verificar la etiqueta y el AAD."""
    if len(password) != 16 or any(c not in PASSWORD_ALPHABET for c in password):
        raise ValidationError(
            "La contraseña AES debe contener 16 letras o números ASCII."
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
            raise ValidationError("Tamaño de archivo R2AE no válido.")
        header = original.read(HEADER_SIZE)
        if len(header) != HEADER_SIZE:
            raise ValidationError("Cabecera R2AE incompleta.")
        magic, version, iterations, salt, nonce = HEADER.unpack(header)
        if (magic, version, iterations) != (MAGIC, VERSION, ITERATIONS):
            raise ValidationError("Cabecera R2AE no válida o versión no compatible.")
        original.seek(-TAG_SIZE, os.SEEK_END)
        tag = original.read(TAG_SIZE)
        if len(tag) != TAG_SIZE:
            raise ValidationError("Etiqueta AES-GCM incompleta.")
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
                    raise ValidationError("Archivo R2AE truncado durante la lectura.")
                remaining -= len(block)
                output.write(decryptor.update(block))
            if original.read(TAG_SIZE) != tag or original.read(1):
                raise ValidationError("El archivo R2AE cambió durante la lectura.")
            try:
                output.write(decryptor.finalize())
            except InvalidTag as error:
                raise ValidationError(
                    "No se pudo autenticar el archivo: contraseña incorrecta "
                    "o contenido/cabecera AAD alterados."
                ) from error
    return destination
