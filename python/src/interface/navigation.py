"""Selección de opciones con teclado o entrada numerada."""

import sys

from ..i18n import tr
from .layout import render
from .terminal import keyboard, read_key


def choose(title, choices, *, fields=(), interactive=False):
    """Solicita una opción por número o mediante navegación por teclado.

    Args:
        title: Título mostrado por layout.render.
        choices: Secuencia no vacía de etiquetas de opción.
        fields: Campos informativos para la composición de la pantalla.
        interactive: Usa teclas y control del cursor en una terminal compatible.

    Returns:
        Índice desde cero, o None al pulsar Esc o introducir una palabra de salida.

    Raises:
        EOFError: Si termina la entrada disponible.
        KeyboardInterrupt: Si se recibe Ctrl+C.

    El modo numerado repite la pregunta ante una entrada inválida. En modo
    interactivo las flechas y j/k recorren las opciones de forma circular.
    """
    selected = 0
    if not interactive:
        print("\n" + render(title, choices, selected, fields, False))
        while True:
            value = input("> ").strip()
            if value.lower() in (
                "q",
                "salir",
                "volver",
                "exit",
                "back",
                "irten",
                "atzera",
            ):
                return None
            if (
                value.isascii()
                and value.isdecimal()
                and 1 <= int(value) <= len(choices)
            ):
                return int(value) - 1
            print(tr("Elige una opción del 1 al {count}.", count=len(choices)))
    with keyboard():
        while True:
            # Limpiar y reposicionar el cursor sin introducir códigos de color.
            sys.stdout.write(
                "\x1b[2J\x1b[H" + render(title, choices, selected, fields, True) + "\n"
            )
            sys.stdout.flush()
            key = read_key()
            if key == "enter":
                return selected
            if key == "escape":
                return None
            if key == "up":
                selected = (selected - 1) % len(choices)
            elif key == "down":
                selected = (selected + 1) % len(choices)
            elif key == "home":
                selected = 0
            elif key == "end":
                selected = len(choices) - 1
            elif key in tuple(str(index + 1) for index in range(len(choices))):
                return int(key) - 1
