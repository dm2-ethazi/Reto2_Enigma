"""Traducciones de la sesión; español por defecto, sin dependencias externas."""

import json
from functools import lru_cache
from pathlib import Path

LANGUAGES = {"es": "Español", "eu": "Euskara", "en": "English"}
# El estado es compartido por la interfaz durante esta ejecución del proceso.
_language = "es"


@lru_cache(maxsize=2)
def _catalog(language: str) -> dict[str, str]:
    """Carga y almacena en caché el catálogo JSON UTF-8 de inglés o euskera."""
    path = Path(__file__).with_name("locales") / f"{language}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def set_language(language: str) -> None:
    """Selecciona el idioma de la sesión sin guardar preferencias en disco.

    Args:
        language: Código es, eu o en. El español utiliza los mensajes originales.

    Raises:
        ValueError: Si el código no pertenece a LANGUAGES.
        OSError: Si no se puede leer el catálogo solicitado.
        json.JSONDecodeError: Si el catálogo no contiene JSON válido.

    El catálogo se carga antes de cambiar el estado para conservar el idioma
    anterior cuando falla la lectura.
    """
    if language not in LANGUAGES:
        raise ValueError(f"Unsupported language: {language}")
    if language != "es":
        # Comprobar la lectura antes de sustituir el idioma activo.
        _catalog(language)
    global _language
    _language = language


def get_language() -> str:
    """Devuelve el código del idioma activo de la sesión: es, eu o en."""
    return _language


def tr(source: str, **values) -> str:
    """Traduce un mensaje y después sustituye sus parámetros con nombre.

    Args:
        source: Mensaje original en español, que también actúa como clave.
        **values: Valores para str.format, como rutas o resultados del usuario.

    Returns:
        Traducción del idioma activo, o source si no existe en el catálogo.
        Sin values se conservan las llaves literales del mensaje.

    Raises:
        KeyError: Si faltan valores para los parámetros de la plantilla.

    Los valores se insertan después de traducir para conservar intactos los datos
    del usuario; las llaves contenidas en ellos no se interpretan de nuevo.
    """
    translated = (
        source if _language == "es" else _catalog(_language).get(source, source)
    )
    return translated.format(**values) if values else translated
