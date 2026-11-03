"""Flujo de navegación entre el menú y las pantallas de presentación."""

from .menu import SCREENS, show_main_menu
from .navigation import choose
from .terminal import enable_cursor_control


def run():
    interactive = enable_cursor_control()
    try:
        while True:
            selected = show_main_menu(interactive=interactive)
            if selected is None or selected == len(SCREENS):
                break
            title, fields = SCREENS[selected]
            choose(
                title,
                ["Volver al menú principal"],
                fields=fields,
                interactive=interactive,
            )
    except (EOFError, KeyboardInterrupt):
        pass
    print("Hasta pronto.")
    return 0
