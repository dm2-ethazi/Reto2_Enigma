"""Compatibilidad de terminal y lectura de teclas en Windows y Linux."""

import os
import select
import sys
from contextlib import contextmanager


def enable_cursor_control():
    """Enable cursor movement only; never change terminal colours."""
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
            and kernel.SetConsoleMode(handle, mode.value | 0x0004)
        )
    except (OSError, AttributeError):
        return False


@contextmanager
def keyboard():
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
        termios.tcsetattr(descriptor, termios.TCSADRAIN, previous)


def read_key():
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
