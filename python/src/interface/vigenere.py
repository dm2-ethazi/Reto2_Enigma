"""Flujo de cifrado y descifrado Vigenère, sin colores."""

import getpass
import sys
from pathlib import Path

from ..encrypt import vigenere
from ..errors import OperationCancelled
from ..utils.dialogs import TEXT_FILES, choose_path
from ..utils.explorer import reveal_file
from ..utils.files import (
    input_text_file,
    new_text_destination,
    temporary_parts,
    write_text,
)
from .loading import loading
from .navigation import choose

SCREENS = (("Cifrar Vigenère", ()), ("Descifrar Vigenère", ()))


def run_encryption(*, interactive: bool = False) -> None:
    _run(interactive=interactive, decrypt=False)


def run_decryption(*, interactive: bool = False) -> None:
    _run(interactive=interactive, decrypt=True)


def _run(*, interactive: bool, decrypt: bool) -> None:
    title = "Descifrar Vigenère" if decrypt else "Cifrar Vigenère"
    progress = "Descifrando" if decrypt else "Cifrando"
    transform = vigenere.decrypt if decrypt else vigenere.encrypt
    transform_file = vigenere.decrypt_file if decrypt else vigenere.encrypt_file
    mode = choose(
        f"{title} / Origen del texto",
        ["Texto por consola", "Archivo .txt"],
        interactive=interactive,
    )
    if mode is None:
        raise OperationCancelled
    source = None
    if mode == 0:
        text = input("Texto: ")
    else:
        source = input_text_file(
            choose_path("Seleccionar archivo de texto", filetypes=TEXT_FILES)
        )

    prompt = "Clave Vigenère (1-64 letras A-Z): "
    key = getpass.getpass(prompt) if sys.stdin.isatty() else input(prompt)
    key = vigenere.validate_key(key)
    mode = choose(
        f"{title} / Qué hacer con el resultado",
        ["Mostrar", "Guardar .txt", "Mostrar y guardar"],
        interactive=interactive,
    )
    if mode is None:
        raise OperationCancelled
    destination = None
    if mode in (1, 2):
        destination = new_text_destination(
            choose_path(
                "Guardar resultado Vigenère",
                save=True,
                default=Path("resultado.txt"),
                filetypes=TEXT_FILES,
                extension=".txt",
            )
        )

    if source is None:
        with loading(progress):
            result = transform(text, key)
        if mode in (0, 2):
            print(f"\nResultado:\n{result}")
        if destination is not None:
            write_text(destination, result)
    else:
        directory = destination.parent if destination is not None else None
        with temporary_parts(directory) as parts:
            with loading(progress):
                for block in transform_file(source, key):
                    parts.write(block)
                parts.finish()
                if destination is not None:
                    parts.publish(destination)
            if mode in (0, 2):
                print("\nResultado:")
                for block in parts.iter_text():
                    sys.stdout.write(block)
                print()
    if destination is not None:
        print(f"Resultado guardado en: {destination}")
        reveal_file(destination)
    input("\nPulsa Enter para volver al menú principal.")
