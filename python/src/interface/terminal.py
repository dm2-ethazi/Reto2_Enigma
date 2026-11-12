"""Compatibilidad de terminal y lectura de teclas en Windows y Linux."""

import os
import select
import sys
from contextlib import contextmanager


def enable_cursor_control():
    """Indica si se puede usar navegación con cursor en esta terminal.

    Rechaza entrada o salida redirigidas y TERM=dumb. En Windows intenta activar
    ENABLE_VIRTUAL_TERMINAL_PROCESSING; devuelve False si la API no está
    disponible o no puede configurarse. No modifica los colores de la terminal.
    """
    if not sys.stdin.isatty() or not sys.stdout.isatty():
        return False
    if os.environ.get("TERM") == "dumb":
        return False
    if os.name != "nt":
        return True
    import ctypes
    from ctypes import wintypes

    try:
        kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        kernel.GetStdHandle.argtypes = [wintypes.DWORD]
        kernel.GetStdHandle.restype = wintypes.HANDLE
        kernel.GetConsoleMode.argtypes = [
            wintypes.HANDLE,
            ctypes.POINTER(wintypes.DWORD),
        ]
        kernel.GetConsoleMode.restype = wintypes.BOOL
        kernel.SetConsoleMode.argtypes = [wintypes.HANDLE, wintypes.DWORD]
        kernel.SetConsoleMode.restype = wintypes.BOOL
        handle = kernel.GetStdHandle(-11)
        mode = wintypes.DWORD()
        return bool(
            kernel.GetConsoleMode(handle, ctypes.byref(mode))
            # Conservar las opciones previas y activar el procesamiento ANSI.
            and kernel.SetConsoleMode(handle, mode.value | 0x0004)
        )
    except (OSError, AttributeError):
        return False


@contextmanager
def keyboard():
    """Cede el control con lectura inmediata de teclas en la plataforma activa.

    En POSIX aplica cbreak y restaura la configuración anterior en finally,
    también cuando el bloque falla. En Windows no cambia el modo de consola,
    porque msvcrt ya permite leer teclas individualmente.
    """
    if os.name == "nt":
        yield
        return
    import termios
    import tty

    descriptor = sys.stdin.fileno()
    previous = termios.tcgetattr(descriptor)
    try:
        tty.setcbreak(descriptor)
        yield
    finally:
        # Restaurar el modo aunque la lectura termine por cancelación o error.
        termios.tcsetattr(descriptor, termios.TCSADRAIN, previous)


def read_key():
    """Normaliza una tecla para la navegación de los menús.

    Returns:
        up, down, home, end, enter o escape para teclas reconocidas; el carácter
        recibido para otras teclas, o una cadena vacía para secuencias ignoradas.

    Raises:
        EOFError: Si se agota la entrada POSIX.
        KeyboardInterrupt: Si se recibe Ctrl+C.

    En POSIX espera brevemente los bytes posteriores a Esc para distinguir una
    cancelación de las secuencias de flechas; en Windows consume el prefijo
    de las teclas extendidas.
    """
    if os.name == "nt":
        import msvcrt

        character = msvcrt.getwch()
        if character in ("\x00", "\xe0"):
            return {"H": "up", "P": "down", "G": "home", "O": "end"}.get(
                msvcrt.getwch(), ""
            )
    else:
        descriptor = sys.stdin.fileno()
        raw = os.read(descriptor, 1)
        if not raw:
            raise EOFError
        character = raw.decode("ascii", errors="ignore")
        if character == "\x1b" and select.select([descriptor], [], [], 0.05)[0]:
            prefix = os.read(descriptor, 1)
            if prefix in (b"[", b"O") and select.select([descriptor], [], [], 0.05)[0]:
                return {b"A": "up", b"B": "down", b"H": "home", b"F": "end"}.get(
                    os.read(descriptor, 1), ""
                )
            return ""
    if character == "\x03":
        raise KeyboardInterrupt
    if character in ("\r", "\n"):
        return "enter"
    return {"\x1b": "escape", "k": "up", "j": "down"}.get(character, character)
