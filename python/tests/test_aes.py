"""Pruebas de cifrado AES, formato R2AE, límites y publicación segura."""

import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from src.encrypt import aes
from src.errors import ValidationError
from src.utils import files


class AesTests(unittest.TestCase):
    """Comprueba el formato R2AE y las garantías del cifrado AES por bloques."""

    def setUp(self):
        """Prepara los recursos aislados de la prueba y registra su limpieza automática."""
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)

    def decrypt_for_verification(self, result):
        """Descifra el contenedor con una ruta de verificación independiente de Enigma."""
        payload = result.path.read_bytes()
        header = payload[:37]
        magic, version, iterations, salt, nonce = aes.HEADER.unpack(header)
        self.assertEqual((magic, version, iterations), (b"R2AE", 1, 200_000))
        key = hashlib.pbkdf2_hmac(
            "sha256", result.password.encode("ascii"), salt, iterations, 32
        )
        return AESGCM(key).decrypt(nonce, payload[37:], header)

    def test_binary_empty_unicode_and_layout(self):
        """Comprueba el cifrado de datos binarios, vacíos y Unicode y el formato R2AE."""
        for index, data in enumerate(
            (b"", b"\x00", bytes(range(256)), "Hola ñ😀\r\n".encode())
        ):
            with self.subTest(size=len(data)):
                source = self.folder / f"original-{index}.bin"
                source.write_bytes(data)
                result = aes.encrypt_file(source)
                self.assertEqual(result.path, Path(f"{source}.enc"))
                self.assertEqual(result.path.stat().st_size, len(data) + 53)
                self.assertEqual(self.decrypt_for_verification(result), data)
                self.assertEqual(source.read_bytes(), data)
                self.assertEqual(len(result.password), 16)
                self.assertTrue(
                    all(c in aes.PASSWORD_ALPHABET for c in result.password)
                )
                self.assertNotIn(result.password, repr(result))
                self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_portable_reference_vectors(self):
        """Verifica los contenedores generados frente a vectores públicos de referencia."""
        fixture = Path(__file__).with_name("fixtures") / "r2ae-v1.json"
        vectors = json.loads(fixture.read_text(encoding="utf-8"))["vectors"]
        for vector in vectors:
            with self.subTest(name=vector["name"]):
                source = self.folder / vector["name"]
                source.write_bytes(bytes.fromhex(vector["plaintext_hex"]))
                with (
                    patch.object(
                        aes, "generate_password", return_value=vector["password"]
                    ),
                    patch.object(
                        aes.secrets,
                        "token_bytes",
                        side_effect=[
                            bytes.fromhex(vector["salt_hex"]),
                            bytes.fromhex(vector["nonce_hex"]),
                        ],
                    ),
                ):
                    result = aes.encrypt_file(source)
                self.assertEqual(
                    result.path.read_bytes(), bytes.fromhex(vector["container_hex"])
                )

    def test_password_generation_uses_secrets(self):
        """Comprueba que la contraseña se construye con secrets y el alfabeto permitido."""
        with patch.object(aes.secrets, "choice", return_value="Z") as choice:
            self.assertEqual(aes.generate_password(), "Z" * 16)
        self.assertEqual(choice.call_count, 16)
        self.assertTrue(
            all(call.args == (aes.PASSWORD_ALPHABET,) for call in choice.call_args_list)
        )

    def test_fresh_password_salt_and_nonce(self):
        """Exige contraseñas, sales y nonces nuevos en cifrados consecutivos."""
        source = self.folder / "original.bin"
        source.write_bytes(b"contenido")
        first = aes.encrypt_file(source, self.folder / "first.enc")
        second = aes.encrypt_file(source, self.folder / "second.enc")
        self.assertNotEqual(first.password, second.password)
        self.assertNotEqual(
            first.path.read_bytes()[9:25], second.path.read_bytes()[9:25]
        )
        self.assertNotEqual(
            first.path.read_bytes()[25:37], second.path.read_bytes()[25:37]
        )

    def test_tampering_or_wrong_password_fails_authentication(self):
        """Rechaza modificaciones del contenedor y contraseñas equivocadas al autenticar."""
        source = self.folder / "original.bin"
        source.write_bytes(b"contenido secreto")
        result = aes.encrypt_file(source)
        payload = result.path.read_bytes()
        header = payload[:37]
        _, _, iterations, salt, nonce = aes.HEADER.unpack(header)
        key = hashlib.pbkdf2_hmac(
            "sha256", result.password.encode(), salt, iterations, 32
        )
        wrong = hashlib.pbkdf2_hmac("sha256", b"wrong-password", salt, iterations, 32)
        with self.assertRaises(InvalidTag):
            AESGCM(wrong).decrypt(nonce, payload[37:], header)
        for offset in (0, 9, 25, 37, len(payload) - 1):
            with self.subTest(offset=offset):
                modified = bytearray(payload)
                modified[offset] ^= 1
                with self.assertRaises(InvalidTag):
                    AESGCM(key).decrypt(
                        nonce, bytes(modified[37:]), bytes(modified[:37])
                    )

    def test_missing_directory_and_existing_sources_or_destinations(self):
        """Rechaza carpetas ausentes y destinos existentes sin modificar los originales."""
        source = self.folder / "original.bin"
        source.write_bytes(b"original")
        with self.assertRaises(FileNotFoundError):
            aes.encrypt_file(self.folder / "ausente")
        with self.assertRaises(ValidationError):
            aes.encrypt_file(self.folder)
        with self.assertRaises(ValidationError):
            aes.encrypt_file(source, self.folder / "ausente" / "out.enc")
        with self.assertRaises(FileExistsError):
            aes.encrypt_file(source, source)
        result = aes.encrypt_file(source)
        container = result.path.read_bytes()
        with self.assertRaises(FileExistsError):
            aes.encrypt_file(source)
        self.assertEqual(source.read_bytes(), b"original")
        self.assertEqual(result.path.read_bytes(), container)

    def test_reads_are_bounded_and_parts_exist_during_encryption(self):
        """Observa lecturas limitadas y temporales antes de publicar el cifrado."""
        source = self.folder / "original.bin"
        destination = self.folder / "out.enc"
        data = bytes(range(256))
        source.write_bytes(data)
        original_open = Path.open
        observations = []

        class Reader:
            """Envuelve el archivo real para observar y limitar las lecturas de la prueba."""

            def __init__(self, stream):
                """Conserva el flujo real que utilizará el lector instrumentado."""
                self.stream = stream

            def __enter__(self):
                """Devuelve el lector instrumentado al entrar en el contexto."""
                return self

            def __exit__(self, *args):
                """Cierra el flujo real al salir del contexto de lectura."""
                self.stream.close()

            def fileno(self):
                """Expone el descriptor real para que las consultas de tamaño sigan funcionando."""
                return self.stream.fileno()

            def read(self, size=-1):
                """Registra y comprueba el tamaño solicitado antes de leer del flujo de prueba."""
                if not 0 < size <= 7:
                    raise AssertionError(f"Lectura sin límite: {size}")
                observations.append(
                    [part.stat().st_size for part in source.parent.glob("*.part")]
                )
                if destination.exists():
                    raise AssertionError(
                        "Destino publicado antes de terminar el cifrado"
                    )
                return self.stream.read(size)

        def open_path(path, *args, **kwargs):
            """Instrumenta solo el archivo origen y conserva la apertura real de los demás."""
            stream = original_open(path, *args, **kwargs)
            return Reader(stream) if path == source else stream

        with (
            patch.object(aes, "CHUNK_SIZE", 7),
            patch.object(files, "MAX_PART_SIZE", 64),
            patch.object(Path, "open", open_path),
        ):
            result = aes.encrypt_file(source, destination)
        self.assertTrue(any(sum(sizes) > 0 for sizes in observations))
        self.assertTrue(all(size <= 64 for sizes in observations for size in sizes))
        self.assertEqual(self.decrypt_for_verification(result), data)
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_real_part_boundaries_include_header_and_tag(self):
        """Incluye cabecera y etiqueta al comprobar los límites reales de las partes."""
        limit = 20 * 1024 * 1024
        self.assertEqual(files.MAX_PART_SIZE, limit)
        for size in (limit - 53, limit - 52, 2 * limit + 13):
            with self.subTest(size=size):
                source = self.folder / f"original-{size}.bin"
                expected = hashlib.sha256()
                remaining = size
                block = bytes(range(256)) * 4096
                with source.open("wb") as stream:
                    while remaining:
                        chunk = block[: min(len(block), remaining)]
                        stream.write(chunk)
                        expected.update(chunk)
                        remaining -= len(chunk)
                sizes = []
                publish = files._publish_parts

                def inspect(parts, destination):
                    """Observa los tamaños de las partes antes de delegar la publicación real."""
                    sizes.extend(part.stat().st_size for part in parts)
                    publish(parts, destination)

                with patch.object(files, "_publish_parts", side_effect=inspect):
                    result = aes.encrypt_file(source)
                self.assertEqual(sum(sizes), size + 53)
                self.assertTrue(all(0 < count <= limit for count in sizes))
                self.assertTrue(all(count == limit for count in sizes[:-1]))
                self.assertEqual(len(sizes), (size + 53 + limit - 1) // limit)
                with result.path.open("rb") as stream:
                    header = stream.read(37)
                    _, _, iterations, salt, nonce = aes.HEADER.unpack(header)
                    stream.seek(-16, 2)
                    tag = stream.read(16)
                    key = hashlib.pbkdf2_hmac(
                        "sha256", result.password.encode(), salt, iterations, 32
                    )
                    decryptor = Cipher(
                        algorithms.AES(key), modes.GCM(nonce, tag)
                    ).decryptor()
                    decryptor.authenticate_additional_data(header)
                    stream.seek(37)
                    actual = hashlib.sha256()
                    remaining = size
                    while remaining:
                        chunk = stream.read(min(1024 * 1024, remaining))
                        self.assertTrue(chunk)
                        actual.update(decryptor.update(chunk))
                        remaining -= len(chunk)
                    actual.update(decryptor.finalize())
                self.assertEqual(actual.digest(), expected.digest())
                self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_collision_at_publish_preserves_other_file(self):
        """Simula una colisión al publicar y conserva el archivo creado por otro proceso."""
        source = self.folder / "original.bin"
        source.write_bytes(b"contenido")
        destination = self.folder / "out.enc"
        publish = files._publish_parts

        def collide(parts, path):
            """Crea un destino ajeno justo antes de publicar para simular una carrera."""
            path.write_bytes(b"otro proceso")
            publish(parts, path)

        for part_limit in (20 * 1024 * 1024, 32):
            with self.subTest(part_limit=part_limit):
                with (
                    patch.object(files, "MAX_PART_SIZE", part_limit),
                    patch.object(files, "_publish_parts", side_effect=collide),
                ):
                    with self.assertRaises(FileExistsError):
                        aes.encrypt_file(source, destination)
                self.assertEqual(destination.read_bytes(), b"otro proceso")
                destination.unlink()
                self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_failed_write_or_interruption_cleans_all_parts(self):
        """Comprueba la limpieza de temporales ante errores de escritura e interrupciones."""
        source = self.folder / "original.bin"
        data = b"contenido secreto" * 20
        source.write_bytes(data)
        for error in (OSError("Disco lleno"), KeyboardInterrupt()):
            with self.subTest(error=type(error).__name__):
                with (
                    patch.object(files, "MAX_PART_SIZE", 64),
                    patch.object(files.os, "fsync", side_effect=error),
                ):
                    with self.assertRaises(type(error)):
                        aes.encrypt_file(source)
                self.assertFalse(Path(f"{source}.enc").exists())
                self.assertEqual(list(self.folder.glob("*.part")), [])
                self.assertEqual(source.read_bytes(), data)

    def test_size_limit_before_start_and_while_reading(self):
        """Comprueba el límite AES-GCM antes y durante la lectura del origen."""
        source = self.folder / "original.bin"
        source.write_bytes(b"contenido")
        with patch.object(aes, "MAX_PLAINTEXT_SIZE", 4):
            with self.assertRaises(ValidationError):
                aes.encrypt_file(source)
            with patch.object(aes.os, "fstat", return_value=SimpleNamespace(st_size=1)):
                with self.assertRaises(ValidationError):
                    aes.encrypt_file(source)
        self.assertFalse(Path(f"{source}.enc").exists())
        self.assertEqual(list(self.folder.glob("*.part")), [])
