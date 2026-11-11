"""Indicador de carga con puntos, sin colores ni dependencias externas."""

import sys
from contextlib import contextmanager
from threading import Event, Thread


@contextmanager
def loading(message: str = "Cifrando", *, interval: float = 0.2):
    output = sys.stdout
    if not output.isatty():
        print(f"{message}...", file=output, flush=True)
        yield
        return

    stopped = Event()

    def show(dots):
        output.write(f"\r{message}{'.' * dots:<3}")
        output.flush()

    def animate():
        dots = 2
        while not stopped.wait(interval):
            show(dots)
            dots = dots % 3 + 1

    show(1)
    animation = Thread(target=animate, name="enigma-loading", daemon=True)
    animation.start()
    completed = False
    try:
        yield
        completed = True
    finally:
        stopped.set()
        animation.join()
        output.write("\r" + " " * (len(message) + 3) + "\r")
        if completed:
            output.write(f"{message}... Completado.\n")
        output.flush()
