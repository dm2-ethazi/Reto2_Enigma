"""Flujo de cifrado y descifrado Vigenère, sin colores."""

import sys
from pathlib import Path

from ..encrypt import vigenere
from ..errors import OperationCancelled
from ..i18n import tr
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
from .passwords import read_password

SCREENS = (("Cifrar Vigenère", ()), ("Descifrar Vigenère", ()))


def run_encryption(*, interactive: bool = False) -> None:
    """Inicia el flujo Vigenère de cifrado con navegación opcional por teclado."""
    _run(interactive=interactive, decrypt=False)


def run_decryption(*, interactive: bool = False) -> None:
    """Inicia el flujo Vigenère de descifrado con navegación opcional por teclado."""
    _run(interactive=interactive, decrypt=True)


def _run(*, interactive: bool, decrypt: bool) -> None:
    """Gestiona origen, clave, destino y presentación del resultado Vigenère.

    Args:
        interactive: Activa la selección mediante teclas en los submenús.
        decrypt: Selecciona descifrado si es True; cifrado en caso contrario.

    El texto de consola se procesa en memoria. Los archivos se procesan en partes
    y solo se muestran o publican tras completar la transformación. Propaga las
    cancelaciones y los errores al bucle principal para comunicarlos al usuario.
    """
    title = tr("Descifrar Vigenère") if decrypt else tr("Cifrar Vigenère")
    progress = tr("Descifrando") if decrypt else tr("Cifrando")
    transform = vigenere.decrypt if decrypt else vigenere.encrypt
    transform_file = vigenere.decrypt_file if decrypt else vigenere.encrypt_file
    mode = choose(
        tr("{title} / Origen del texto", title=title),
        [tr("Texto por consola"), tr("Archivo .txt")],
        interactive=interactive,
    )
    if mode is None:
        raise OperationCancelled
    source = None
    if mode == 0:
        text = input(tr("Texto: "))
    else:
        source = input_text_file(
            choose_path(tr("Seleccionar archivo de texto"), filetypes=TEXT_FILES)
        )

    prompt = tr("Clave Vigenère (1-64 letras A-Z): ")
    key = read_password(prompt)
    key = vigenere.validate_key(key)
    mode = choose(
        tr("{title} / Qué hacer con el resultado", title=title),
        [tr("Mostrar"), tr("Guardar .txt"), tr("Mostrar y guardar")],
        interactive=interactive,
    )
    if mode is None:
        raise OperationCancelled
    destination = None
    if mode in (1, 2):
        destination = new_text_destination(
            choose_path(
                tr("Guardar resultado Vigenère"),
                save=True,
                default=Path("resultado.txt"),
                filetypes=TEXT_FILES,
                extension=".txt",
            )
        )

    if source is None:
        with loading(progress):
            result = transform(text, key)
            if destination is not None:
                write_text(destination, result)
        if mode in (0, 2):
            print(tr("\nResultado:\n{result}", result=result))
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
                print(tr("\nResultado:"))
                for block in parts.iter_text():
                    sys.stdout.write(block)
                print()
    if destination is not None:
        print(tr("Resultado guardado en: {destination}", destination=destination))
        reveal_file(destination)
    input(tr("\nPulsa Enter para volver al menú principal."))
