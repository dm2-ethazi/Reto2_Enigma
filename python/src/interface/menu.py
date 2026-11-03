"""Opciones y presentación del menú principal."""

from .navigation import choose
from .vigenere import SCREENS

CHOICES = [title for title, _ in SCREENS] + ["Salir"]


def show_main_menu(*, interactive=False):
    return choose("Menú principal", CHOICES, interactive=interactive)
