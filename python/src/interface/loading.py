"""Indicador de carga con puntos, sin colores ni dependencias externas."""

import sys
from contextlib import contextmanager
from threading import Event, Thread

from ..i18n import tr


@contextmanager
def loading(message: str = "Cifrando", *, interval: float = 0.2):
    """Muestra progreso durante una operación y detiene la animación al salir.

    Args:
        message: Mensaje o clave de traducción que precede a los puntos.
        interval: Segundos entre actualizaciones del hilo de animación.

    Yields:
        Control al bloque que realiza la operación.

    Con salida redirigida imprime una línea sin iniciar hilos. En una terminal
    limpia el indicador al terminar y solo anuncia éxito si el bloque no falla.
    Las excepciones del bloque se propagan después de detener el hilo.
    """
    message = tr(message)
    output = sys.stdout
    if not output.isatty():
        print(f"{message}...", file=output, flush=True)
        yield
        return

    stopped = Event()

    def show(dots):
        """Redibuja el indicador y vacía la salida sin avanzar de línea."""
        output.write(f"\r{message}{'.' * dots:<3}")
        output.flush()

    def animate():
        """Alterna entre uno y tres puntos hasta recibir la señal de parada."""
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
        # Esperar al hilo antes de limpiar evita que vuelva a dibujar el indicador.
        animation.join()
        output.write("\r" + " " * (len(message) + 3) + "\r")
        if completed:
            output.write(tr("{message}... Completado.\n", message=message))
        output.flush()
