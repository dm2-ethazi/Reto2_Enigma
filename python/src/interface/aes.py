"""Selección de archivos y cifrado AES, sin colores."""

from pathlib import Path

from ..errors import ValidationError
from ..utils.dialogs import ENCRYPTED_FILES, choose_path
from .loading import loading

SCREENS = (("Cifrar archivo AES", ()),)


def run_encryption(*, interactive: bool = False) -> None:
    try:
        from ..encrypt import aes
    except ModuleNotFoundError as error:
        if error.name == "cryptography":
            raise ValidationError(
                "AES necesita cryptography. Instala requirements.txt en el entorno "
                "de Python que ejecuta Enigma: python -m pip install -r requirements.txt"
            ) from error
        raise

    source = choose_path("Seleccionar archivo para cifrar con AES")
    destination = choose_path(
        "Guardar archivo cifrado",
        save=True,
        default=Path(f"{source}.enc"),
        filetypes=ENCRYPTED_FILES,
        extension=".enc",
    )
    with loading("Cifrando AES"):
        result = aes.encrypt_file(source, destination)
    print(f"Archivo cifrado: {result.path}")
    print(f"Contraseña generada: {result.password}")
    print("Guarda esta contraseña: no se almacena en el archivo ni se puede recuperar.")
    input("\nPulsa Enter para volver al menú principal.")
