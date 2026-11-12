"""Entrada de contraseñas con un asterisco por carácter, sin mostrar el secreto."""

import os
import sys

from ..errors import OperationCancelled
from .terminal import keyboard


def _read_character():
    """Lee un carácter sin eco usando la API de la plataforma.

    Consume las teclas extendidas de Windows y devuelve una cadena vacía para
    ignorarlas. En POSIX conserva Unicode y lanza EOFError si termina la entrada.
    """
    if os.name == "nt":
        import msvcrt

        character = msvcrt.getwch()
        if character in ("\x00", "\xe0"):
            msvcrt.getwch()
            return ""
        return character
    character = sys.stdin.read(1)
    if not character:
        raise EOFError
    return character


def read_password(prompt: str) -> str:
    """Lee una contraseña mostrando un asterisco por carácter imprimible.

    Args:
        prompt: Pregunta que se escribe antes de recoger la entrada.

    Returns:
        Contraseña tal como se introdujo, sin validarla ni eliminar espacios.

    Raises:
        OperationCancelled: Si el usuario pulsa Esc en modo interactivo.
        EOFError: Si termina la entrada o se recibe una tecla de fin de archivo.
        KeyboardInterrupt: Si el usuario pulsa Ctrl+C.

    Retroceso elimina el último carácter. Restaura el modo de terminal al salir.
    Con entrada redirigida utiliza input en lugar de capturar teclas sin eco.
    """
    if not sys.stdin.isatty():
        return input(prompt)
    output = sys.stdout
    characters = []
    output.write(prompt)
    output.flush()
    try:
        with keyboard():
            while True:
                character = _read_character()
                if character in ("\r", "\n"):
                    return "".join(characters)
                if character == "\x03":
                    raise KeyboardInterrupt
                if character in ("\x04", "\x1a"):
                    raise EOFError
                if character == "\x1b":
                    raise OperationCancelled
                if character in ("\b", "\x7f"):
                    if characters:
                        characters.pop()
                        # Retroceder, borrar el asterisco y recuperar su posición.
                        output.write("\b \b")
                        output.flush()
                elif character and character.isprintable():
                    characters.append(character)
                    output.write("*")
                    output.flush()
    finally:
        output.write("\n")
        output.flush()
