"""Menú de cifrado y descifrado con errores y cancelación controlados."""

import logging

from ..errors import EnigmaError, OperationCancelled
from ..i18n import get_language, tr
from ..logging_config import configure_logging
from .aes import run_encryption as run_aes_encryption
from .aes import run_decryption as run_aes_decryption
from .menu import SCREENS, change_language, show_main_menu
from .terminal import enable_cursor_control
from .vigenere import run_encryption
from .vigenere import run_decryption

logger = logging.getLogger("enigma")


def run():
    """Ejecuta el menú hasta salir, recibir EOF o interrumpir con Ctrl+C.

    Returns:
        0 tras cerrar la sesión de terminal.

    Comunica errores controlados en el idioma activo y permite volver al menú.
    Las cancelaciones no requieren confirmación y conservan la sesión.
    """
    logger = configure_logging()
    logger.info("Application started; language=%s", get_language())
    interactive = enable_cursor_control()
    try:
        while True:
            selected = show_main_menu(interactive=interactive)
            if selected is None or selected == len(SCREENS):
                break
            try:
                if selected == 0:
                    run_encryption(interactive=interactive)
                elif selected == 1:
                    run_decryption(interactive=interactive)
                elif selected == 2:
                    run_aes_encryption(interactive=interactive)
                elif selected == 3:
                    run_aes_decryption(interactive=interactive)
                elif selected == len(SCREENS) + 1:
                    change_language(interactive=interactive)
            except OperationCancelled:
                logger.info("Operation cancelled by user")
                print(tr("Operación cancelada. Volviendo al menú."))
            except (EnigmaError, OSError, UnicodeError, MemoryError) as error:
                # No guardar str(error): algunos errores incluyen rutas privadas.
                logger.error("Operation failed; error_type=%s", type(error).__name__)
                message = str(error) or tr(
                    "Recursos insuficientes para completar la operación."
                )
                if isinstance(error, UnicodeError):
                    message = tr("El archivo de texto no contiene UTF-8 válido.")
                elif isinstance(error, MemoryError):
                    message = tr("Recursos insuficientes para completar la operación.")
                elif (
                    isinstance(error, OSError)
                    and error.errno is not None
                    and get_language() != "es"
                ):
                    # El texto de OSError depende del sistema; traducir por tipo
                    # y conservar la ruta sin pasar sus datos por el catálogo.
                    if isinstance(error, FileNotFoundError):
                        message = tr("El archivo o carpeta no existe.")
                    elif isinstance(error, PermissionError):
                        message = tr(
                            "Permiso denegado para acceder al archivo o carpeta."
                        )
                    else:
                        message = tr("No se pudo acceder al archivo o carpeta.")
                    if error.filename:
                        message += f" {error.filename}"
                print(tr("Error: {message}", message=message))
                input(tr("\nPulsa Enter para volver al menú principal."))
            except Exception as error:
                # Registrar fallos inesperados por tipo, sin volcar rutas ni secretos.
                logger.error(
                    "Unexpected operation failure; error_type=%s", type(error).__name__
                )
                raise
    except (EOFError, KeyboardInterrupt):
        logger.info("Application interrupted or input closed")
    print(tr("Hasta pronto."))
    logger.info("Application stopped")
    return 0
