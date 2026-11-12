"""Selección de archivos, cifrado y descifrado autenticado AES, sin colores."""

from pathlib import Path

from ..errors import ValidationError
from ..i18n import tr
from ..utils.dialogs import ENCRYPTED_FILES, choose_path
from ..utils.explorer import reveal_file
from .loading import loading
from .passwords import read_password

SCREENS = (("Cifrar archivo AES", ()), ("Descifrar archivo AES", ()))


def run_encryption(*, interactive: bool = False) -> None:
    """Ejecuta la selección de archivos y el cifrado AES con contraseña aleatoria.

    interactive se admite por compatibilidad con los demás flujos; los selectores
    y las preguntas de AES no dependen del control del cursor.
    """
    _run(decrypt=False)


def run_decryption(*, interactive: bool = False) -> None:
    """Ejecuta el descifrado autenticado AES solicitando la contraseña original.

    interactive mantiene la firma común de las operaciones de la interfaz.
    """
    _run(decrypt=True)


def _run(*, decrypt: bool) -> None:
    """Coordina AES: seleccionar rutas, procesar, mostrar resultado y esperar Enter.

    decrypt elige la dirección. Propaga cancelaciones y errores al bucle principal;
    si falta cryptography, los convierte en una validación con ayuda de instalación.
    La contraseña generada solo se muestra después de guardar correctamente.
    """
    try:
        from ..encrypt import aes
    except ModuleNotFoundError as error:
        if error.name == "cryptography":
            raise ValidationError(
                tr(
                    "AES necesita cryptography. Instala requirements.txt en el entorno "
                    "de Python que ejecuta Enigma: python -m pip install -r requirements.txt"
                )
            ) from error
        raise

    if decrypt:
        source = choose_path(
            tr("Seleccionar archivo para descifrar con AES"), filetypes=ENCRYPTED_FILES
        )
        prompt = tr("Contraseña AES (16 letras o números): ")
        password = read_password(prompt)
        default = (
            source.with_suffix("")
            if source.suffix.lower() == ".enc"
            else Path(f"{source}.decrypted")
        )
        destination = choose_path(
            tr("Guardar archivo descifrado"), save=True, default=default
        )
        with loading(tr("Descifrando AES y verificando autenticidad")):
            path = aes.decrypt_file(source, password, destination)
        print(tr("Archivo descifrado y autenticado: {path}", path=path))
        reveal_file(path)
        input(tr("\nPulsa Enter para volver al menú principal."))
        return

    source = choose_path(tr("Seleccionar archivo para cifrar con AES"))
    destination = choose_path(
        tr("Guardar archivo cifrado"),
        save=True,
        default=Path(f"{source}.enc"),
        filetypes=ENCRYPTED_FILES,
        extension=".enc",
    )
    with loading(tr("Cifrando AES")):
        result = aes.encrypt_file(source, destination)
    print(tr("Archivo cifrado: {path}", path=result.path))
    print(tr("Contraseña generada: {password}", password=result.password))
    print(
        tr(
            "Guarda esta contraseña: no se almacena en el archivo ni se puede recuperar."
        )
    )
    reveal_file(result.path)
    input(tr("\nPulsa Enter para volver al menú principal."))
