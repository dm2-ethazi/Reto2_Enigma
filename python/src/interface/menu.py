"""Opciones y presentación del menú principal."""

from .aes import SCREENS as AES_SCREENS
from .navigation import choose
from .vigenere import SCREENS as VIGENERE_SCREENS

SCREENS = (VIGENERE_SCREENS[0], AES_SCREENS[0], VIGENERE_SCREENS[1], AES_SCREENS[1])
CHOICES = [title for title, _ in SCREENS] + ["Salir"]


def show_main_menu(*, interactive=False):
    return choose("Menú principal", CHOICES, interactive=interactive)
