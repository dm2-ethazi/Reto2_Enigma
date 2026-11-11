"""Entrada de contraseñas con un asterisco por carácter, sin mostrar el secreto."""

import os
import sys

from ..errors import OperationCancelled
from .terminal import keyboard


def _read_character():
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
                        output.write("\b \b")
                        output.flush()
                elif character and character.isprintable():
                    characters.append(character)
                    output.write("*")
                    output.flush()
    finally:
        output.write("\n")
        output.flush()
