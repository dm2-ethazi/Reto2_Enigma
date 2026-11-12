"""Opciones y presentación del menú principal."""

from ..i18n import LANGUAGES, set_language
from .aes import SCREENS as AES_SCREENS
from .navigation import choose
from .vigenere import SCREENS as VIGENERE_SCREENS

SCREENS = VIGENERE_SCREENS + AES_SCREENS
CHOICES = [title for title, _ in SCREENS] + ["Salir", "Idioma / Hizkuntza / Language"]


def show_main_menu(*, interactive=False):
    return choose("Menú principal", CHOICES, interactive=interactive)


def change_language(*, interactive=False):
    selected = choose(
        "Idioma / Hizkuntza / Language",
        list(LANGUAGES.values()),
        interactive=interactive,
    )
    if selected is not None:
        set_language(list(LANGUAGES)[selected])
