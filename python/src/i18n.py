"""Traducciones de la sesión; español por defecto, sin dependencias externas."""

import json
from functools import lru_cache
from pathlib import Path

LANGUAGES = {"es": "Español", "eu": "Euskara", "en": "English"}
_language = "es"


@lru_cache(maxsize=2)
def _catalog(language: str) -> dict[str, str]:
    path = Path(__file__).with_name("locales") / f"{language}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def set_language(language: str) -> None:
    """Cambia el idioma hasta cerrar la aplicación; rechaza códigos desconocidos."""
    if language not in LANGUAGES:
        raise ValueError(f"Unsupported language: {language}")
    if language != "es":
        _catalog(language)
    global _language
    _language = language


def get_language() -> str:
    return _language


def tr(source: str, **values) -> str:
    """Traduce antes de interpolar rutas, resultados u otros datos del usuario."""
    translated = (
        source if _language == "es" else _catalog(_language).get(source, source)
    )
    return translated.format(**values) if values else translated
