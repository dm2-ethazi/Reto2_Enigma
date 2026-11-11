import hashlib
import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from src.encrypt import aes, vigenere
from src.errors import OperationCancelled, ValidationError
from src.interface import app
from src.interface import aes as aes_screen
from src.interface import vigenere as vig_screen
from src.utils import explorer, files


class DecryptionTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.folder = Path(temp.name)
        self.source = self.folder / "source.bin"
        self.destination = self.folder / "restored.bin"
        self.source.write_bytes(bytes(range(256)) * 4)
        self.encrypted = aes.encrypt_file(self.source)

    def assert_clean(self):
        self.assertFalse(self.destination.exists())
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_reference_containers_including_empty_and_binary(self):
        vectors = json.loads(
            (Path(__file__).parent / "fixtures/r2ae-v1.json").read_text()
        )["vectors"]
        for vector in vectors:
            with self.subTest(name=vector["name"]):
                container = self.folder / (vector["name"] + ".enc")
                container.write_bytes(bytes.fromhex(vector["container_hex"]))
                restored = aes.decrypt_file(container, vector["password"])
                self.assertEqual(
                    restored.read_bytes(), bytes.fromhex(vector["plaintext_hex"])
                )
                self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_tampered_header_ciphertext_tag_and_wrong_password_never_publish(self):
        original = self.encrypted.path.read_bytes()
        for offset in (0, 4, 5, 9, 25, 37, len(original) - 1):
            payload = bytearray(original)
            payload[offset] ^= 1
            self.encrypted.path.write_bytes(payload)
            with self.subTest(offset=offset), self.assertRaises(ValidationError):
                aes.decrypt_file(
                    self.encrypted.path, self.encrypted.password, self.destination
                )
            self.assert_clean()
        self.encrypted.path.write_bytes(original)
        for password in ("Z" * 16, "", "a" * 15, "ñ" * 16, " " * 16):
            with self.subTest(password=password), self.assertRaises(ValidationError):
                aes.decrypt_file(self.encrypted.path, password, self.destination)
            self.assert_clean()

    def test_requires_exact_header_as_aad(self):
        payload = self.encrypted.path.read_bytes()
        header = payload[: aes.HEADER_SIZE]
        _, _, iterations, salt, nonce = aes.HEADER.unpack(header)
        key = hashlib.pbkdf2_hmac(
            "sha256", self.encrypted.password.encode(), salt, iterations, 32
        )
        for aad in (None, header[:-1], b"other header"):
            self.encrypted.path.write_bytes(
                header + AESGCM(key).encrypt(nonce, b"secret", aad)
            )
            with (
                self.subTest(aad=aad),
                self.assertRaisesRegex(ValidationError, "autenticar"),
            ):
                aes.decrypt_file(
                    self.encrypted.path, self.encrypted.password, self.destination
                )
            self.assert_clean()

    def test_rejects_truncated_extra_bytes_and_unbounded_kdf_parameters(self):
        payload = self.encrypted.path.read_bytes()
        for malformed in (
            b"",
            payload[:52],
            payload[:-1],
            payload + b"x",
            payload[:5] + b"\xff" * 4 + payload[9:],
        ):
            self.encrypted.path.write_bytes(malformed)
            with self.subTest(size=len(malformed)), self.assertRaises(ValidationError):
                aes.decrypt_file(
                    self.encrypted.path, self.encrypted.password, self.destination
                )
            self.assert_clean()
        with patch.object(aes, "_derive_key") as derive:
            with self.assertRaises(ValidationError):
                aes.decrypt_file(
                    self.encrypted.path, self.encrypted.password, self.destination
                )
            derive.assert_not_called()

    def test_streaming_parts_and_publish_only_after_authentication(self):
        original_open = Path.open
        source = self.encrypted.path
        sizes = []
        destination = self.destination

        class Reader:
            def __init__(self, stream):
                self.stream = stream

            def __enter__(self):
                return self

            def __exit__(self, *args):
                self.stream.close()

            def fileno(self):
                return self.stream.fileno()

            def seek(self, *args):
                return self.stream.seek(*args)

            def read(self, size=-1):
                if not 0 < size <= aes.HEADER_SIZE:
                    raise AssertionError(f"Unbounded read: {size}")
                if destination.exists():
                    raise AssertionError("Published before authentication")
                sizes.extend(p.stat().st_size for p in source.parent.glob("*.part"))
                return self.stream.read(size)

        def open_path(path, *args, **kwargs):
            stream = original_open(path, *args, **kwargs)
            return Reader(stream) if path == source else stream

        with (
            patch.object(Path, "open", open_path),
            patch.object(aes, "CHUNK_SIZE", 7),
            patch.object(files, "MAX_PART_SIZE", 64),
        ):
            result = aes.decrypt_file(source, self.encrypted.password, destination)
        self.assertEqual(result.read_bytes(), self.source.read_bytes())
        self.assertTrue(any(sizes))
        self.assertTrue(all(size <= 64 for size in sizes))
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_real_twenty_mib_parts(self):
        data = b"abc\x00" * (files.MAX_PART_SIZE // 4 + 7)
        self.source.write_bytes(data)
        self.encrypted.path.unlink()
        encrypted = aes.encrypt_file(self.source)
        publish = files._publish_parts
        sizes = []

        def inspect(parts, destination):
            sizes.extend(p.stat().st_size for p in parts)
            publish(parts, destination)

        with patch.object(files, "_publish_parts", side_effect=inspect):
            aes.decrypt_file(encrypted.path, encrypted.password, self.destination)
        self.assertEqual(sizes, [files.MAX_PART_SIZE, 28])
        self.assertEqual(self.destination.read_bytes(), data)
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_existing_output_failure_and_cancel_preserve_source(self):
        payload = self.encrypted.path.read_bytes()
        with self.assertRaises(FileExistsError):
            aes.decrypt_file(self.encrypted.path, self.encrypted.password, self.source)
        for error in (OSError("Disk full"), KeyboardInterrupt()):
            with (
                patch.object(files, "MAX_PART_SIZE", 32),
                patch.object(files.os, "fsync", side_effect=error),
            ):
                with self.assertRaises(type(error)):
                    aes.decrypt_file(
                        self.encrypted.path, self.encrypted.password, self.destination
                    )
            self.assert_clean()
        self.assertEqual(self.encrypted.path.read_bytes(), payload)

    def test_non_enc_filename_and_size_limit(self):
        source = self.encrypted.path.rename(self.folder / "container.bin")
        result = aes.decrypt_file(source, self.encrypted.password)
        self.assertEqual(result.name, "container.bin.decrypted")
        self.assertEqual(result.read_bytes(), self.source.read_bytes())
        with (
            patch.object(aes, "MAX_PLAINTEXT_SIZE", 2),
            self.assertRaises(ValidationError),
        ):
            aes.decrypt_file(source, self.encrypted.password, self.destination)
        self.assert_clean()

    def test_vigenere_known_vector_and_utf8_split_at_every_byte(self):
        self.assertEqual(vigenere.decrypt("LXFOPVEFRNHR", "LEMON"), "ATTACKATDAWN")
        text = "\ufeffAttack at dawn! ñé😀\r\nZz 123"
        encrypted = vigenere.encrypt(text, "LeMoN").encode()
        for split in range(len(encrypted) + 1):
            with self.subTest(split=split):
                actual = b"".join(
                    vigenere.decrypt_chunks(
                        [encrypted[:split], encrypted[split:]], "LEMON"
                    )
                )
                self.assertEqual(actual, text.encode())
        with self.assertRaises(UnicodeDecodeError):
            list(vigenere.decrypt_chunks([b"abc\xc3"], "A"))
        with self.assertRaises(ValidationError):
            vigenere.decrypt("abc", "")


class DecryptionInterfaceTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.folder = Path(temp.name)

    def run_app(self, answers, screen, paths):
        with (
            patch.object(app, "enable_cursor_control", return_value=False),
            patch.object(screen.sys.stdin, "isatty", return_value=False),
            patch("builtins.input", side_effect=answers),
            patch.object(screen, "choose_path", side_effect=paths),
            patch.object(screen, "reveal_file") as reveal,
            redirect_stdout(io.StringIO()) as output,
        ):
            self.assertEqual(app.run(), 0)
        return output.getvalue(), reveal

    def test_vigenere_console_decrypt(self):
        output, reveal = self.run_app(
            ["2", "1", "LXFOPVEFRNHR", "LEMON", "1", "", "5"], vig_screen, []
        )
        self.assertIn("Resultado:\nATTACKATDAWN", output)
        self.assertIn("Descifrando...", output)
        reveal.assert_not_called()

    def test_vigenere_decrypt_file_with_parts_and_explorer(self):
        source = self.folder / "encrypted.txt"
        destination = self.folder / "plain.txt"
        text = "\ufeffHello ñ😀\r\n" * 10
        source.write_bytes(vigenere.encrypt(text, "LEMON").encode())
        with (
            patch.object(files, "MAX_PART_SIZE", 8),
            patch.object(vigenere, "FILE_BLOCK_SIZE", 3),
        ):
            output, reveal = self.run_app(
                ["2", "2", "LEMON", "3", "", "5"], vig_screen, [source, destination]
            )
        self.assertEqual(destination.read_bytes(), text.encode())
        self.assertIn("Resultado guardado", output)
        reveal.assert_called_once_with(destination)
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_aes_decrypt_success_wrong_password_and_cancellation(self):
        source = self.folder / "plain.bin"
        source.write_bytes(b"\x00secret\xff")
        encrypted = aes.encrypt_file(source)
        destination = self.folder / "restored.bin"
        output, reveal = self.run_app(
            ["4", encrypted.password, "", "5"],
            aes_screen,
            [encrypted.path, destination],
        )
        self.assertEqual(destination.read_bytes(), source.read_bytes())
        self.assertIn("descifrado y autenticado", output)
        reveal.assert_called_once_with(destination)
        destination.unlink()
        output, reveal = self.run_app(
            ["4", "Z" * 16, "", "5"], aes_screen, [encrypted.path, destination]
        )
        self.assertIn("Error:", output)
        self.assertNotIn("descifrado y autenticado", output)
        reveal.assert_not_called()
        self.assertFalse(destination.exists())
        self.assertEqual(list(self.folder.glob("*.part")), [])
        output, reveal = self.run_app(["4", "5"], aes_screen, [OperationCancelled()])
        self.assertIn("Operación cancelada", output)
        reveal.assert_not_called()

    def test_encrypt_opens_explorer_only_after_success(self):
        source = self.folder / "plain.bin"
        source.write_bytes(b"secret")
        destination = self.folder / "out.enc"
        _, reveal = self.run_app(["3", "", "5"], aes_screen, [source, destination])
        reveal.assert_called_once_with(destination)
        _, reveal = self.run_app(["3", "", "5"], aes_screen, [source, destination])
        reveal.assert_not_called()


class ExplorerTests(unittest.TestCase):
    def test_platform_launch_arguments_and_no_shell(self):
        path = Path("folder with spaces/file;name.txt").absolute()
        for platform, command in [
            ("win32", ["explorer.exe", "/select,", str(path)]),
            ("darwin", ["open", "-R", str(path)]),
            ("linux", ["xdg-open", str(path.parent)]),
        ]:
            with (
                self.subTest(platform=platform),
                patch.object(explorer.sys, "platform", platform),
                patch.object(explorer.subprocess, "Popen") as launch,
            ):
                self.assertTrue(explorer.reveal_file(path))
                self.assertEqual(launch.call_args.args[0], command)
                self.assertFalse(launch.call_args.kwargs.get("shell", False))

    def test_launch_failure_reports_saved_path(self):
        with (
            patch.object(
                explorer.subprocess, "Popen", side_effect=OSError("No desktop")
            ),
            redirect_stdout(io.StringIO()) as output,
        ):
            self.assertFalse(explorer.reveal_file("result.txt"))
        self.assertIn("Archivo guardado en:", output.getvalue())
