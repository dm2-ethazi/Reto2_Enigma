"""Menú de cifrado y descifrado con errores y cancelación controlados."""

from ..errors import EnigmaError, OperationCancelled
from .aes import run_encryption as run_aes_encryption
from .aes import run_decryption as run_aes_decryption
from .menu import SCREENS, show_main_menu
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
                    run_aes_encryption(interactive=interactive)
                elif selected == 2:
                    run_decryption(interactive=interactive)
                elif selected == 3:
                    run_aes_decryption(interactive=interactive)
            except OperationCancelled:
                print("Operación cancelada. Volviendo al menú.")
            except (EnigmaError, OSError, UnicodeError, MemoryError) as error:
                message = (
                    str(error) or "Recursos insuficientes para completar la operación."
                )
                print(f"Error: {message}")
                input("\nPulsa Enter para volver al menú principal.")
    except (EOFError, KeyboardInterrupt):
        pass
    print("Hasta pronto.")
    return 0
