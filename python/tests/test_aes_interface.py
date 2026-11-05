import hashlib
import io
import re
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from src.encrypt import aes
from src.errors import OperationCancelled
from src.interface import aes as screen
from src.interface import app
from src.utils import files


class AesInterfaceTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)
        self.source = self.folder / "archivo con espacios.bin"
        self.data = bytes(range(256))
        self.source.write_bytes(self.data)
        self.destination = self.folder / "archivo cifrado.enc"

    def run_app(self, answers, paths):
        output = io.StringIO()
        with (
            patch.object(app, "enable_cursor_control", return_value=False),
            patch("builtins.input", side_effect=answers),
            patch.object(screen, "choose_path", side_effect=paths) as picker,
            redirect_stdout(output),
        ):
            self.assertEqual(app.run(), 0)
        self.assertNotIn("\x1b", output.getvalue())
        return output.getvalue(), picker

    def test_encrypt_via_menu_and_show_password_after_success(self):
        output, picker = self.run_app(["2", "", "3"], [self.source, self.destination])
        password = re.search(r"Contraseña generada: ([A-Za-z0-9]{16})", output).group(1)
        payload = self.destination.read_bytes()
        header = payload[:37]
        _, _, iterations, salt, nonce = aes.HEADER.unpack(header)
        key = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, iterations, 32)
        self.assertEqual(AESGCM(key).decrypt(nonce, payload[37:], header), self.data)
        self.assertEqual(self.source.read_bytes(), self.data)
        self.assertIn("Cifrando AES...", output)
        self.assertIn(f"Archivo cifrado: {self.destination}", output)
        self.assertIn("Hasta pronto.", output)
        self.assertEqual(picker.call_args_list[1].kwargs["default"], Path(f"{self.source}.enc"))
        self.assertEqual(picker.call_args_list[1].kwargs["extension"], ".enc")
        self.assertTrue(picker.call_args_list[1].kwargs["save"])
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_cancel_either_picker_does_not_start_encryption(self):
        for paths in ([OperationCancelled()], [self.source, OperationCancelled()]):
            with self.subTest(paths=len(paths)), patch.object(aes, "encrypt_file") as encrypt:
                output, _ = self.run_app(["2", "3"], paths)
                encrypt.assert_not_called()
                self.assertIn("Operación cancelada", output)
                self.assertNotIn("Contraseña generada:", output)
                self.assertFalse(self.destination.exists())

    def test_existing_destination_returns_to_menu(self):
        self.destination.write_bytes(b"otro archivo")
        output, _ = self.run_app(["2", "", "3"], [self.source, self.destination])
        self.assertIn("Error:", output)
        self.assertNotIn("Contraseña generada:", output)
        self.assertEqual(self.destination.read_bytes(), b"otro archivo")
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_disk_error_does_not_report_success_or_password(self):
        with patch.object(files.os, "fsync", side_effect=OSError("Disco lleno")):
            output, _ = self.run_app(["2", "", "3"], [self.source, self.destination])
        self.assertIn("Error: Disco lleno", output)
        self.assertNotIn("Contraseña generada:", output)
        self.assertNotIn("Archivo cifrado:", output)
        self.assertFalse(self.destination.exists())
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_missing_source_returns_to_menu(self):
        output, _ = self.run_app(
            ["2", "", "3"], [self.folder / "ausente.bin", self.destination]
        )
        self.assertIn("Error:", output)
        self.assertNotIn("Contraseña generada:", output)
        self.assertFalse(self.destination.exists())
