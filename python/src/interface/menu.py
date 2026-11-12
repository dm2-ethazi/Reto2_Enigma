"""Opciones y presentación del menú principal."""

import logging

from ..i18n import LANGUAGES, set_language
from .aes import SCREENS as AES_SCREENS
from .navigation import choose
from .vigenere import SCREENS as VIGENERE_SCREENS

SCREENS = VIGENERE_SCREENS + AES_SCREENS
# Mantener Salir en la quinta posición conserva la navegación numerada existente.
CHOICES = [title for title, _ in SCREENS] + ["Salir", "Idioma / Hizkuntza / Language"]
logger = logging.getLogger("enigma")


def show_main_menu(*, interactive=False):
    """Devuelve el índice de la opción principal elegida, o None al cancelar."""
    return choose("Menú principal", CHOICES, interactive=interactive)


def change_language(*, interactive=False):
    """Muestra los nombres nativos y aplica el idioma elegido a la sesión.

    Utiliza el orden de LANGUAGES para asociar índices y códigos. Al cancelar
    conserva el idioma anterior sin guardar preferencias en disco.
    """
    selected = choose(
        "Idioma / Hizkuntza / Language",
        list(LANGUAGES.values()),
        interactive=interactive,
    )
    if selected is not None:
        set_language(list(LANGUAGES)[selected])
        logger.info("Language selected; language=%s", list(LANGUAGES)[selected])
