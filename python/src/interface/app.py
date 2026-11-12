"""Menú de cifrado y descifrado con errores y cancelación controlados."""

from ..errors import EnigmaError, OperationCancelled
from ..i18n import get_language, tr
from .aes import run_encryption as run_aes_encryption
from .aes import run_decryption as run_aes_decryption
from .menu import SCREENS, change_language, show_main_menu
from .terminal import enable_cursor_control
from .vigenere import run_encryption
from .vigenere import run_decryption


def run():
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
                print(tr("Operación cancelada. Volviendo al menú."))
            except (EnigmaError, OSError, UnicodeError, MemoryError) as error:
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
    except (EOFError, KeyboardInterrupt):
        pass
    print(tr("Hasta pronto."))
    return 0
