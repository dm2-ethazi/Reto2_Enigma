"""Lanzador de la interfaz de Enigma; la aplicación reside en src."""

import argparse

from src.i18n import LANGUAGES, set_language
from src.interface.app import run


def main(argv=None):
    """Configura el idioma inicial y ejecuta la interfaz de terminal.

    Args:
        argv: Argumentos sin el nombre del programa; None utiliza sys.argv.

    Returns:
        Código de salida de la aplicación, normalmente 0.

    Raises:
        SystemExit: Si argparse recibe --help o argumentos no válidos.
    """
    parser = argparse.ArgumentParser(description="Enigma — Español / Euskara / English")
    parser.add_argument(
        "--lang",
        choices=LANGUAGES,
        default="es",
        help="Idioma / Hizkuntza / Language: es, eu, en (es)",
    )
    args = parser.parse_args(argv)
    set_language(args.lang)
    return run()


if __name__ == "__main__":
    raise SystemExit(main())
