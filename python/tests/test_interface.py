import io
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

from src.errors import OperationCancelled
from src.interface import app, layout
from src.interface import vigenere as screen
from src.utils import files


class InterfaceTests(unittest.TestCase):
    def run_app(self, answers, *, paths=()):
        output = io.StringIO()
        with (
            patch.object(app, "enable_cursor_control", return_value=False),
            patch.object(screen, "reveal_file"),
            patch.object(screen.sys.stdin, "isatty", return_value=False),
            patch("builtins.input", side_effect=answers),
            patch.object(screen, "choose_path", side_effect=paths),
            redirect_stdout(output),
        ):
            self.assertEqual(app.run(), 0)
        self.assertNotRegex(output.getvalue(), r"\x1b\[[0-9;]*m")
        return output.getvalue()

    def test_console_encrypt(self):
        output = self.run_app(
            [
                "1",
                "1",
                "ATTACKATDAWN",
                "LEMON",
                "1",
                "",
                "5",
            ]
        )
        self.assertIn("Resultado:\nLXFOPVEFRNHR", output)
        self.assertLess(output.index("Cifrando..."), output.index("Resultado:\n"))
        self.assertIn("Descifrar Vigenère", output)

    def test_file_show_and_save(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "entrada.txt"
            destination = Path(folder) / "salida.txt"
            source.write_bytes("\ufeffHola ñ\r\n".encode("utf-8"))
            output = self.run_app(
                ["1", "2", "B", "3", "", "5"], paths=[source, destination]
            )
            self.assertEqual(
                destination.read_bytes(), "\ufeffIpmb ñ\r\n".encode("utf-8")
            )
            self.assertIn("Resultado guardado", output)
            self.assertIn("Resultado:\n", output)

    def test_file_creates_parts_while_encrypting_without_bulk_reads(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "entrada.txt"
            destination = Path(folder) / "salida.txt"
            text = "\ufeffATTACKATDAWN\r\nñ😀AbZ" * 9
            data = text.encode("utf-8")
            source.write_bytes(data)
            expected = screen.vigenere.encrypt(text, "LEMON").encode("utf-8")
            encrypt_file = screen.vigenere.encrypt_file
            observations = []

            def observe_parts(path, key):
                for block in encrypt_file(path, key):
                    observations.append(
                        [part.stat().st_size for part in Path(folder).glob("*.part")]
                    )
                    self.assertFalse(destination.exists())
                    yield block

            with (
                patch.object(screen.vigenere, "FILE_BLOCK_SIZE", 5),
                patch.object(files, "MAX_PART_SIZE", 8),
                patch.object(
                    screen.vigenere, "encrypt_file", side_effect=observe_parts
                ),
                patch.object(
                    screen.vigenere,
                    "encrypt",
                    side_effect=AssertionError("Cifrado en memoria"),
                ),
                patch.object(
                    Path, "read_bytes", side_effect=AssertionError("Lectura completa")
                ),
                patch.object(
                    Path, "read_text", side_effect=AssertionError("Lectura completa")
                ),
            ):
                output = self.run_app(
                    ["1", "2", "LEMON", "2", "", "5"], paths=[source, destination]
                )
            self.assertEqual(destination.read_bytes(), expected)
            self.assertEqual(source.read_bytes(), data)
            self.assertTrue(any(sizes and sum(sizes) > 0 for sizes in observations))
            self.assertTrue(all(size <= 8 for sizes in observations for size in sizes))
            self.assertEqual(list(Path(folder).glob("*.part")), [])
            self.assertIn("Resultado guardado", output)

    def test_file_show_only_reads_parts_and_removes_them(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "entrada.txt"
            source.write_bytes("\ufeffATTACKATDAWN\r\nñ😀".encode("utf-8"))
            with (
                patch.object(files.tempfile, "gettempdir", return_value=folder),
                patch.object(files, "MAX_PART_SIZE", 8),
                patch.object(screen.vigenere, "FILE_BLOCK_SIZE", 3),
            ):
                output = self.run_app(["1", "2", "LEMON", "1", "", "5"], paths=[source])
            self.assertIn("Resultado:\n\ufeffLXFOPVEFRNHR\r\nñ😀", output)
            self.assertNotIn("Resultado guardado", output)
            self.assertEqual(list(Path(folder).iterdir()), [source])

    def test_invalid_utf8_after_written_blocks_cleans_parts(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "entrada.txt"
            destination = Path(folder) / "salida.txt"
            source.write_bytes(b"ATTACKATDAWN\xc3")
            with (
                patch.object(files, "MAX_PART_SIZE", 8),
                patch.object(screen.vigenere, "FILE_BLOCK_SIZE", 3),
            ):
                output = self.run_app(
                    ["1", "2", "LEMON", "2", "", "5"], paths=[source, destination]
                )
            self.assertIn("Error:", output)
            self.assertFalse(destination.exists())
            self.assertEqual(list(Path(folder).glob("*.part")), [])

    def test_interrupt_during_file_cipher_cleans_written_parts(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "entrada.txt"
            destination = Path(folder) / "salida.txt"
            source.write_bytes(b"ATTACKATDAWN")

            def interrupt(path, key):
                yield b"LXFOPVEF"
                self.assertTrue(list(Path(folder).glob("*.part")))
                raise KeyboardInterrupt

            with patch.object(screen.vigenere, "encrypt_file", side_effect=interrupt):
                output = self.run_app(
                    ["1", "2", "LEMON", "2"], paths=[source, destination]
                )
            self.assertIn("Hasta pronto.", output)
            self.assertFalse(destination.exists())
            self.assertEqual(list(Path(folder).glob("*.part")), [])

    def test_console_save_without_showing_result(self):
        with tempfile.TemporaryDirectory() as folder:
            destination = Path(folder) / "salida.txt"
            output = self.run_app(
                ["1", "1", "ATTACKATDAWN", "LEMON", "2", "", "5"], paths=[destination]
            )
            self.assertEqual(destination.read_text(encoding="utf-8"), "LXFOPVEFRNHR")
            self.assertNotIn("Resultado:\n", output)

    def test_invalid_key_returns_to_menu(self):
        output = self.run_app(["1", "1", "texto", "ñ", "", "5"])
        self.assertIn("Error: La clave Vigenère", output)
        self.assertIn("Hasta pronto.", output)

    def test_cancel_submenus_and_file_picker(self):
        for answers, paths in (
            (["1", "q", "5"], []),
            (["1", "1", "texto", "A", "q", "5"], []),
            (["1", "2", "5"], [OperationCancelled()]),
            (["1", "1", "texto", "A", "2", "5"], [OperationCancelled()]),
        ):
            with self.subTest(answers=answers):
                self.assertIn("Operación cancelada", self.run_app(answers, paths=paths))

    def test_missing_file_returns_to_menu(self):
        with tempfile.TemporaryDirectory() as folder:
            output = self.run_app(
                ["1", "2", "", "5"], paths=[Path(folder) / "ausente.txt"]
            )
        self.assertIn("Error:", output)
        self.assertIn("Hasta pronto.", output)

    def test_interactive_key_is_hidden_and_result_waits_for_acknowledgement(self):
        with (
            patch.object(screen, "choose", side_effect=[0, 0]),
            patch.object(screen.sys.stdin, "isatty", return_value=True),
            patch.object(screen.getpass, "getpass", return_value="LEMON") as secret,
            patch("builtins.input", side_effect=["ATTACKATDAWN", ""]) as prompt,
            redirect_stdout(io.StringIO()) as output,
        ):
            screen.run_encryption(interactive=True)
        secret.assert_called_once()
        self.assertEqual(prompt.call_count, 2)
        self.assertIn("LXFOPVEFRNHR", output.getvalue())

    def test_submenus_have_no_main_menu_groups_or_colours(self):
        for interactive in (False, True):
            output = layout.render(
                "Origen del texto", ["Consola", "Archivo"], 0, (), interactive
            )
            self.assertNotIn("AES / ARCHIVOS", output)
            self.assertNotIn("VIGENÈRE / TEXTO", output)
            self.assertNotRegex(output, r"\x1b\[[0-9;]*m")

    def test_menu_offers_encryption_decryption_and_exit(self):
        output = self.run_app(["5"])
        self.assertIn("1. Cifrar Vigenère", output)
        self.assertIn("3. Cifrar archivo AES", output)
        self.assertIn("2. Descifrar Vigenère", output)
        self.assertIn("4. Descifrar archivo AES", output)
        self.assertIn("5. Salir", output)
        self.assertIn("Hasta pronto.", output)

    def test_main_menu_keeps_each_algorithm_under_its_heading(self):
        for interactive in (False, True):
            from src.interface.menu import CHOICES

            output = layout.render("Menú principal", CHOICES, 0, (), interactive)
            positions = [
                output.index(label)
                for label in (
                    "VIGENÈRE / TEXTO",
                    "1. Cifrar Vigenère",
                    "2. Descifrar Vigenère",
                    "AES / ARCHIVOS",
                    "3. Cifrar archivo AES",
                    "4. Descifrar archivo AES",
                    "5. Salir",
                )
            ]
            self.assertEqual(positions, sorted(positions))
