import io
import unittest
from contextlib import redirect_stdout
from threading import Event, Thread
from unittest.mock import patch

from src.interface.loading import loading


class LoadingTests(unittest.TestCase):
    def test_redirected_output_has_one_plain_message(self):
        output = io.StringIO()
        with redirect_stdout(output), loading():
            print("Resultado")
        self.assertEqual(output.getvalue(), "Cifrando...\nResultado\n")

    def test_animation_cycles_and_stops_on_completion_error_or_cancellation(self):
        for error in (None, ValueError("Clave incorrecta"), KeyboardInterrupt()):
            with self.subTest(error=type(error).__name__):
                ready = Event()
                frames = []
                threads = []

                class TerminalOutput(io.StringIO):
                    def isatty(self):
                        return True

                    def write(self, text):
                        count = super().write(text)
                        if text.startswith("\rCifrando"):
                            frames.append(text.strip())
                            if len(frames) >= 4:
                                ready.set()
                        return count

                def create_thread(*args, **kwargs):
                    thread = Thread(*args, **kwargs)
                    threads.append(thread)
                    return thread

                output = TerminalOutput()

                def operation():
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
