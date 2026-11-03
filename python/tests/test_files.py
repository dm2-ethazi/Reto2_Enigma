import hashlib
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from src.encrypt import vigenere
from src.errors import ValidationError
from src.utils import files


class FileTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)

    def test_encrypt_file_preserves_bom_line_endings_and_source(self):
        source = self.folder / "entrada.TXT"
        data = "\ufeffHola ñ\r\nsiguiente\n".encode("utf-8")
        source.write_bytes(data)
        encrypted = self.folder / "cifrado.txt"
        files.write_text(encrypted, vigenere.encrypt(files.read_text(source), "B"))
        self.assertEqual(source.read_bytes(), data)
        self.assertEqual(encrypted.read_bytes(), "\ufeffIpmb ñ\r\ntjhvjfouf\n".encode("utf-8"))

    def test_rejects_invalid_files(self):
        with self.assertRaises(FileNotFoundError):
            files.read_text(self.folder / "ausente.txt")
        with self.assertRaises(ValidationError):
            files.read_text(self.folder)
        binary = self.folder / "entrada.bin"
        binary.write_bytes(b"abc")
        with self.assertRaises(ValidationError):
            files.read_text(binary)
        with self.assertRaises(ValidationError):
            files.write_text(binary, "texto")
        invalid = self.folder / "incorrecto.txt"
        invalid.write_bytes(b"\xff")
        with self.assertRaises(UnicodeDecodeError):
            files.read_text(invalid)
        with self.assertRaises(ValidationError):
            files.write_text(self.folder / "ausente" / "salida.txt", "texto")

    def test_existing_destination_is_not_overwritten(self):
        target = self.folder / "salida.txt"
        target.write_bytes(b"original")
        with self.assertRaises(FileExistsError):
            files.write_text(target, "nuevo")
        self.assertEqual(target.read_bytes(), b"original")

    def test_destination_created_during_write_is_not_overwritten(self):
        for limit in (20 * 1024 * 1024, 4):
            with self.subTest(limit=limit), patch.object(files, "MAX_PART_SIZE", limit):
                target = self.folder / f"salida-{limit}.txt"
                with self.assertRaises(FileExistsError):
                    with files.staged_output(target) as output:
                        output.write(b"nuevo")
                        target.write_bytes(b"otro proceso")
                self.assertEqual(target.read_bytes(), b"otro proceso")
                self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_failed_publish_cleans_temporary_file(self):
        target = self.folder / "salida.txt"
        with patch.object(files.os, "link", side_effect=PermissionError("Acceso")):
            with self.assertRaises(PermissionError):
                files.write_text(target, "texto")
        self.assertEqual(list(self.folder.iterdir()), [])

    def test_part_boundaries_and_multiple_write_calls(self):
        publish = files._publish_parts
        for size in (0, 7, 8, 9, 16, 17):
            for write_size in (3, 32):
                with self.subTest(size=size, write_size=write_size):
                    target = self.folder / f"salida-{size}-{write_size}.txt"
                    data = bytes(range(size))
                    observed = []

                    def inspect_parts(parts, destination):
                        observed.extend(part.stat().st_size for part in parts)
                        self.assertEqual(b"".join(part.read_bytes() for part in parts), data)
                        self.assertFalse(destination.exists())
                        publish(parts, destination)

                    with (
                        patch.object(files, "MAX_PART_SIZE", 8),
                        patch.object(files, "_publish_parts", side_effect=inspect_parts),
                    ):
                        with files.staged_output(target) as output:
                            for offset in range(0, size, write_size):
                                chunk = data[offset : offset + write_size]
                                self.assertEqual(output.write(chunk), len(chunk))
                    expected_sizes = [8] * (size // 8)
                    if size % 8 or size == 0:
                        expected_sizes.append(size % 8)
                    self.assertEqual(observed, expected_sizes)
                    self.assertEqual(target.read_bytes(), data)
                    self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_utf8_characters_can_cross_part_boundaries(self):
        target = self.folder / "unicode.txt"
        text = "\ufeffá😀ñ\r\n" * 5
        with patch.object(files, "MAX_PART_SIZE", 8), patch.object(files, "COPY_CHUNK_SIZE", 2):
            files.write_text(target, text)
        self.assertEqual(target.read_bytes(), text.encode("utf-8"))
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_multipart_failure_or_interruption_removes_incomplete_destination(self):
        real_fsync = files.os.fsync
        for error in (OSError("Disco lleno"), KeyboardInterrupt()):
            with self.subTest(error=type(error).__name__):
                target = self.folder / "salida.txt"

                def fail_on_destination(descriptor):
                    if target.exists():
                        self.assertEqual(target.read_bytes(), b"abcdefghijklmnopqrst")
                        raise error
                    real_fsync(descriptor)

                with (
                    patch.object(files, "MAX_PART_SIZE", 8),
                    patch.object(files.os, "fsync", side_effect=fail_on_destination),
                ):
                    with self.assertRaises(type(error)):
                        with files.staged_output(target) as output:
                            output.write(b"abcdefghijklmnopqrst")
                self.assertEqual(list(self.folder.iterdir()), [])

    def test_failure_syncing_part_cleans_all_parts(self):
        target = self.folder / "salida.txt"
        with (
            patch.object(files, "MAX_PART_SIZE", 8),
            patch.object(files.os, "fsync", side_effect=OSError("Disco lleno")),
        ):
            with self.assertRaises(OSError):
                with files.staged_output(target) as output:
                    output.write(b"abcdefghijklmnopqrst")
        self.assertEqual(list(self.folder.iterdir()), [])

    def test_cancel_before_publish_cleans_all_parts(self):
        target = self.folder / "salida.txt"
        with patch.object(files, "MAX_PART_SIZE", 8):
            with self.assertRaises(KeyboardInterrupt):
                with files.staged_output(target) as output:
                    output.write(b"abcdefghijklmnopqrst")
                    raise KeyboardInterrupt
        self.assertEqual(list(self.folder.iterdir()), [])

    def test_real_20_mib_limit_with_three_parts(self):
        self.assertEqual(files.MAX_PART_SIZE, 20 * 1024 * 1024)
        target = self.folder / "grande.txt"
        expected_hash = hashlib.sha256()
        publish = files._publish_parts
        observed = []

        def inspect_parts(parts, destination):
            observed.extend(part.stat().st_size for part in parts)
            publish(parts, destination)

        with patch.object(files, "_publish_parts", side_effect=inspect_parts):
            with files.staged_output(target) as output:
                for index in range(41):
                    block = bytes([index]) * (1024 * 1024)
                    expected_hash.update(block)
                    output.write(block)
                    for part in self.folder.glob("*.part"):
                        self.assertLessEqual(part.stat().st_size, files.MAX_PART_SIZE)
                output.write(b"tail")
                expected_hash.update(b"tail")
        self.assertEqual(observed, [20 * 1024 * 1024, 20 * 1024 * 1024, 1024 * 1024 + 4])
        self.assertEqual(target.stat().st_size, 41 * 1024 * 1024 + 4)
        with target.open("rb") as stream:
            self.assertEqual(hashlib.file_digest(stream, "sha256").digest(), expected_hash.digest())
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_exact_20_mib_boundary_and_next_byte(self):
        publish = files._publish_parts
        block = b"x" * (1024 * 1024)
        for extra in (0, 1):
            with self.subTest(extra=extra):
                target = self.folder / f"limite-{extra}.txt"
                observed = []

                def inspect_parts(parts, destination):
                    observed.extend(part.stat().st_size for part in parts)
                    publish(parts, destination)

                with patch.object(files, "_publish_parts", side_effect=inspect_parts):
                    with files.staged_output(target) as output:
                        for _ in range(20):
                            output.write(block)
                        if extra:
                            output.write(b"y")
                self.assertEqual(observed, [20 * 1024 * 1024] + ([1] if extra else []))
                self.assertEqual(target.stat().st_size, 20 * 1024 * 1024 + extra)
                with target.open("rb") as result:
                    for _ in range(20):
                        self.assertEqual(result.read(len(block)), block)
                    self.assertEqual(result.read(), b"y" if extra else b"")
                self.assertEqual(list(self.folder.glob("*.part")), [])
