"""Pruebas de lectura enmascarada de claves en Windows, POSIX y entrada redirigida."""

import io
import unittest
from contextlib import nullcontext, redirect_stdout
from types import SimpleNamespace
from unittest.mock import patch

from src.errors import OperationCancelled
from src.interface import passwords


class PasswordTests(unittest.TestCase):
    """Comprueba lectura, edición y enmascarado de secretos sin usar la terminal real."""

    def test_windows_reader_consumes_extended_keys_without_adding_a_character(self):
        """Consume los códigos de teclas extendidas de Windows sin incorporarlos al secreto."""
        console = SimpleNamespace(
            getwch=unittest.mock.Mock(side_effect=["\xe0", "K", "j"])
        )
        with (
            patch.object(passwords.os, "name", "nt"),
            patch.dict("sys.modules", msvcrt=console),
        ):
            self.assertEqual(passwords._read_character(), "")
            self.assertEqual(passwords._read_character(), "j")
        self.assertEqual(console.getwch.call_count, 3)

    def test_posix_reader_preserves_unicode_and_detects_eof(self):
        """Lee caracteres Unicode en POSIX y detecta el final de la entrada."""
        with (
            patch.object(passwords.os, "name", "posix"),
            patch.object(passwords.sys, "stdin", io.StringIO("ñ")),
        ):
            self.assertEqual(passwords._read_character(), "ñ")
            with self.assertRaises(EOFError):
                passwords._read_character()

    def read(self, characters):
        """Simula teclas de una contraseña y devuelve el secreto y la salida capturada."""
        output = io.StringIO()
        with (
            patch.object(passwords.sys.stdin, "isatty", return_value=True),
            patch.object(passwords, "keyboard", return_value=nullcontext()),
            patch.object(passwords, "_read_character", side_effect=characters),
            redirect_stdout(output),
        ):
            result = passwords.read_password("Clave: ")
        return result, output.getvalue()

    def test_masks_each_character_and_backspace_edits_without_echo(self):
        """Enmascara cada carácter y permite corregirlo con Retroceso sin mostrar el secreto."""
        value, output = self.read(
            ["\b", "a", "b", "\b", "j", "k", "Ñ", "\x7f", "Z", "\r"]
        )
        self.assertEqual(value, "ajkZ")
        self.assertEqual(output, "Clave: **\b \b***\b \b*\n")
        self.assertNotIn("ajkZ", output)

    def test_pasted_password_and_ignored_control_keys(self):
        """Acepta contraseñas pegadas e ignora controles que no representan caracteres."""
        value, output = self.read(["", "\t", *"AbCdEf0123456789", "\n"])
        self.assertEqual(value, "AbCdEf0123456789")
        self.assertEqual(output, "Clave: " + "*" * 16 + "\n")

    def test_cancel_eof_interrupt_restore_terminal(self):
        """Restaura el contexto de terminal tras cancelar, recibir EOF o interrumpir."""
        for character, error in [
            ("\x1b", OperationCancelled),
            ("\x03", KeyboardInterrupt),
            ("\x04", EOFError),
            ("\x1a", EOFError),
            (EOFError(), EOFError),
        ]:
            output = io.StringIO()
            with (
                self.subTest(character=repr(character)),
                patch.object(passwords.sys.stdin, "isatty", return_value=True),
                patch.object(passwords, "keyboard") as keyboard,
                patch.object(
                    passwords, "_read_character", side_effect=["a", character]
                ),
                redirect_stdout(output),
            ):
                with self.assertRaises(error):
                    passwords.read_password("Clave: ")
                keyboard.return_value.__exit__.assert_called_once()
            self.assertEqual(output.getvalue(), "Clave: *\n")

    def test_redirected_input_uses_plain_input_without_raw_terminal(self):
        """Utiliza input directamente cuando la contraseña llega desde entrada redirigida."""
        with (
            patch.object(passwords.sys.stdin, "isatty", return_value=False),
            patch("builtins.input", return_value="LEMON") as prompt,
            patch.object(passwords, "keyboard") as keyboard,
        ):
            self.assertEqual(passwords.read_password("Clave: "), "LEMON")
            prompt.assert_called_once_with("Clave: ")
            keyboard.assert_not_called()
