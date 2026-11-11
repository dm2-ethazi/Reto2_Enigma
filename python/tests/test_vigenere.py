import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from src.encrypt import vigenere
from src.errors import ValidationError


class VigenereTests(unittest.TestCase):
    def test_known_vector(self):
        self.assertEqual(vigenere.encrypt("ATTACKATDAWN", "LEMON"), "LXFOPVEFRNHR")
        self.assertEqual(vigenere.encrypt("ATTACKATDAWN", "lemon"), "LXFOPVEFRNHR")

    def test_encryption_preserves_case_unicode_and_line_endings(self):
        text = "\ufeff¡Hola, Señor! Mañana: café, pingüino. ÁÉÍÓÚ ñÑ 123 😀\r\nAbZ\tfin"
        for key in ("a", "Z", "MiClave", "z" * 64):
            with self.subTest(key=key):
                encrypted = vigenere.encrypt(text, key)
                for original, transformed in zip(text, encrypted, strict=True):
                    if not original.isascii() or not original.isalpha():
                        self.assertEqual(transformed, original)
                    else:
                        self.assertEqual(original.isupper(), transformed.isupper())

    def test_only_ascii_letters_advance_key(self):
        self.assertEqual(vigenere.encrypt("A ñ-Á1 a\nA", "BC"), "B ñ-Á1 c\nB")
        self.assertEqual(vigenere.encrypt("", "A"), "")

    def test_invalid_keys(self):
        for key in ("", "a" * 65, "ñ", "Á", "ABC DEF", "A1", "A!", "K", "ß"):
            with self.subTest(key=key):
                with self.assertRaises(ValidationError):
                    vigenere.encrypt("texto", key)

    def test_stream_matches_text_cipher_at_every_byte_boundary(self):
        text = "\ufeffATTACKATDAWN\r\n¡Hola, Señor! 😀 ñÁ 123\nAz" * 7
        data = text.encode("utf-8")
        for block_size in (1, 2, 3, 7, 16, 31):
            for key in ("LEMON", "a", "MiClave", "Z" * 64):
                with self.subTest(block_size=block_size, key=key):
                    blocks = (
                        data[offset : offset + block_size]
                        for offset in range(0, len(data), block_size)
                    )
                    encrypted = b"".join(vigenere.encrypt_chunks(blocks, key))
                    self.assertEqual(
                        encrypted, vigenere.encrypt(text, key).encode("utf-8")
                    )

    def test_stream_rejects_invalid_and_truncated_utf8(self):
        for data in (b"Hola\xff", b"Hola\xc3", b"Hola\xf0\x9f\x98"):
            with self.subTest(data=data), self.assertRaises(UnicodeDecodeError):
                list(
                    vigenere.encrypt_chunks(
                        (bytes([character]) for character in data), "KEY"
                    )
                )

    def test_stream_validates_key_even_when_empty(self):
        self.assertEqual(list(vigenere.encrypt_chunks([], "KEY")), [])
        with self.assertRaises(ValidationError):
            list(vigenere.encrypt_chunks([], ""))

    def test_file_uses_bounded_reads(self):
        data = "\ufeffATTACKATDAWN\r\nñ😀AbZ".encode("utf-8") * 9
        reads = []

        class BoundedReader(io.BytesIO):
            def read(self, size=-1):
                if not 0 < size <= 7:
                    raise AssertionError(f"Lectura sin límite: {size}")
                reads.append(size)
                return super().read(size)

        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "entrada.txt"
            source.write_bytes(data)
            with (
                patch.object(vigenere, "FILE_BLOCK_SIZE", 7),
                patch.object(Path, "open", return_value=BoundedReader(data)),
            ):
                result = b"".join(vigenere.encrypt_file(source, "LEMON"))
            self.assertEqual(
                result, vigenere.encrypt(data.decode("utf-8"), "LEMON").encode("utf-8")
            )
        self.assertGreater(len(reads), 1)
