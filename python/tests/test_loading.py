"""Pruebas del progreso animado y de la parada de su hilo de trabajo."""

import io
import unittest
from contextlib import redirect_stdout
from threading import Event, Thread
from unittest.mock import patch

from src.interface.loading import loading


class LoadingTests(unittest.TestCase):
    """Comprueba salida y ciclo de vida del indicador de progreso."""

    def test_redirected_output_has_one_plain_message(self):
        """Emite un solo mensaje de progreso sin animación cuando la salida se redirige."""
        output = io.StringIO()
        with redirect_stdout(output), loading():
            print("Resultado")
        self.assertEqual(output.getvalue(), "Cifrando...\nResultado\n")

    def test_animation_cycles_and_stops_on_completion_error_or_cancellation(self):
        """Detiene el hilo de animación tanto al completar como al fallar o cancelar."""
        for error in (None, ValueError("Clave incorrecta"), KeyboardInterrupt()):
            with self.subTest(error=type(error).__name__):
                ready = Event()
                frames = []
                threads = []

                class TerminalOutput(io.StringIO):
                    """Simula una terminal y registra los fotogramas del indicador de progreso."""

                    def isatty(self):
                        """Identifica la salida simulada como una terminal interactiva."""
                        return True

                    def write(self, text):
                        """Captura cada actualización y avisa cuando se ha observado un ciclo de puntos."""
                        count = super().write(text)
                        if text.startswith("\rCifrando"):
                            frames.append(text.strip())
                            if len(frames) >= 4:
                                ready.set()
                        return count

                def create_thread(*args, **kwargs):
                    """Registra el hilo real de animación para comprobar que termina."""
                    thread = Thread(*args, **kwargs)
                    threads.append(thread)
                    return thread

                output = TerminalOutput()

                def operation():
                    """Ejecuta el indicador hasta observar un ciclo y provoca el error solicitado."""
                    with (
                        patch(
                            "src.interface.loading.Thread", side_effect=create_thread
                        ),
                        redirect_stdout(output),
                        loading(interval=0.001),
                    ):
                        self.assertTrue(
                            ready.wait(timeout=2), "El indicador no se actualizó."
                        )
                        if error is not None:
                            raise error

                if error is None:
                    operation()
                else:
                    with self.assertRaises(type(error)):
                        operation()
                self.assertEqual(
                    frames[:4], ["Cifrando.", "Cifrando..", "Cifrando...", "Cifrando."]
                )
                self.assertTrue(threads)
                self.assertTrue(all(not thread.is_alive() for thread in threads))
                ending = (
                    "Cifrando... Completado.\n"
                    if error is None
                    else "\r" + " " * 11 + "\r"
                )
                self.assertTrue(output.getvalue().endswith(ending))
                if error is not None:
                    self.assertNotIn("Completado", output.getvalue())
                self.assertNotIn("\x1b", output.getvalue())
