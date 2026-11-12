"""Pruebas de descifrado autenticado, Vigenère y apertura del explorador."""

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
    """Verifica autenticación, integridad y publicación del descifrado por bloques."""

    def setUp(self):
        """Prepara los recursos aislados de la prueba y registra su limpieza automática."""
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.folder = Path(temp.name)
        self.source = self.folder / "source.bin"
        self.destination = self.folder / "restored.bin"
        self.source.write_bytes(bytes(range(256)) * 4)
        self.encrypted = aes.encrypt_file(self.source)

    def assert_clean(self):
        """Comprueba que no se publicó el destino ni quedan partes temporales."""
        self.assertFalse(self.destination.exists())
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_reference_containers_including_empty_and_binary(self):
        """Descifra vectores R2AE de referencia con contenido vacío y binario."""
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
        """Impide publicar al alterar cabecera, contenido, etiqueta o contraseña."""
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
        """Exige autenticar los bytes exactos de la cabecera como AAD."""
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
        """Rechaza truncados, bytes sobrantes y parámetros KDF ajenos al formato admitido."""
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
        """Verifica el descifrado por bloques y la publicación posterior a autenticar."""
        original_open = Path.open
        source = self.encrypted.path
        sizes = []
        destination = self.destination

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

            def seek(self, *args):
                """Delega el desplazamiento en el flujo real para consultar cabecera y etiqueta."""
                return self.stream.seek(*args)

            def read(self, size=-1):
                """Registra y comprueba el tamaño solicitado antes de leer del flujo de prueba."""
                if not 0 < size <= aes.HEADER_SIZE:
                    raise AssertionError(f"Unbounded read: {size}")
                if destination.exists():
                    raise AssertionError("Published before authentication")
                sizes.extend(p.stat().st_size for p in source.parent.glob("*.part"))
                return self.stream.read(size)

        def open_path(path, *args, **kwargs):
            """Instrumenta solo el archivo origen y conserva la apertura real de los demás."""
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
        """Comprueba el descifrado con temporales limitados a 20 MiB reales."""
        data = b"abc\x00" * (files.MAX_PART_SIZE // 4 + 7)
        self.source.write_bytes(data)
        self.encrypted.path.unlink()
        encrypted = aes.encrypt_file(self.source)
        publish = files._publish_parts
        sizes = []

        def inspect(parts, destination):
            """Observa los tamaños de las partes antes de delegar la publicación real."""
            sizes.extend(p.stat().st_size for p in parts)
            publish(parts, destination)

        with patch.object(files, "_publish_parts", side_effect=inspect):
            aes.decrypt_file(encrypted.path, encrypted.password, self.destination)
        self.assertEqual(sizes, [files.MAX_PART_SIZE, 28])
        self.assertEqual(self.destination.read_bytes(), data)
        self.assertEqual(list(self.folder.glob("*.part")), [])

    def test_existing_output_failure_and_cancel_preserve_source(self):
        """Conserva el origen ante destinos existentes, fallos e interrupciones."""
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
        """Comprueba el destino por defecto sin extensión .enc y el límite de tamaño."""
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
        """Descifra un vector Vigenère y texto UTF-8 dividido en bloques de un byte."""
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
    """Comprueba los flujos de descifrado Vigenère y AES desde el menú."""

    def setUp(self):
        """Prepara los recursos aislados de la prueba y registra su limpieza automática."""
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.folder = Path(temp.name)

    def run_app(self, answers, screen, paths):
        """Ejecuta la interfaz con respuestas y rutas simuladas y captura su salida."""
        with (
            patch.object(app, "enable_cursor_control", return_value=False),
            patch("src.interface.passwords.sys.stdin.isatty", return_value=False),
            patch("builtins.input", side_effect=answers),
            patch.object(screen, "choose_path", side_effect=paths),
            patch.object(screen, "reveal_file") as reveal,
            redirect_stdout(io.StringIO()) as output,
        ):
            self.assertEqual(app.run(), 0)
        return output.getvalue(), reveal

    def test_vigenere_console_decrypt(self):
        """Descifra texto por consola desde el menú y muestra el resultado esperado."""
        output, reveal = self.run_app(
            ["2", "1", "LXFOPVEFRNHR", "LEMON", "1", "", "5"], vig_screen, []
        )
        self.assertIn("Resultado:\nATTACKATDAWN", output)
        self.assertIn("Descifrando...", output)
        reveal.assert_not_called()

    def test_vigenere_decrypt_file_with_parts_and_explorer(self):
        """Descifra un archivo por partes y muestra el resultado en el explorador."""
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
        """Cubre descifrado AES correcto, contraseña errónea y cancelaciones de interfaz."""
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
        """Abre el explorador solo después de un cifrado publicado correctamente."""
        source = self.folder / "plain.bin"
        source.write_bytes(b"secret")
        destination = self.folder / "out.enc"
        _, reveal = self.run_app(["3", "", "5"], aes_screen, [source, destination])
        reveal.assert_called_once_with(destination)
        _, reveal = self.run_app(["3", "", "5"], aes_screen, [source, destination])
        reveal.assert_not_called()


class ExplorerTests(unittest.TestCase):
    """Comprueba el lanzamiento del explorador y su alternativa ante errores."""

    def test_platform_launch_arguments_and_no_shell(self):
        """Comprueba los argumentos del explorador para cada plataforma sin usar shell."""
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
        """Muestra la ruta conservada cuando no se puede iniciar el explorador."""
        with (
            patch.object(
                explorer.subprocess, "Popen", side_effect=OSError("No desktop")
            ),
            redirect_stdout(io.StringIO()) as output,
        ):
            self.assertFalse(explorer.reveal_file("result.txt"))
        self.assertIn("Archivo guardado en:", output.getvalue())
