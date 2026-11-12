"""Pruebas de selectores Tkinter y rutas introducidas sin escritorio."""

import io
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from types import ModuleType
from unittest.mock import Mock, patch

from src.errors import OperationCancelled
from src.utils import dialogs


class DialogTests(unittest.TestCase):
    """Prueba los selectores nativos simulados y la entrada alternativa por terminal."""

    def setUp(self):
        """Prepara los recursos aislados de la prueba y registra su limpieza automática."""
        tk = ModuleType("tkinter")
        tk.TclError = type("TclError", (Exception,), {})
        self.root = Mock()
        tk.Tk = Mock(return_value=self.root)
        self.native = ModuleType("tkinter.filedialog")
        self.native.askopenfilename = Mock(return_value="")
        self.native.asksaveasfilename = Mock(return_value="")
        tk.filedialog = self.native
        self.tk = tk
        modules = patch.dict(
            sys.modules, {"tkinter": tk, "tkinter.filedialog": self.native}
        )
        modules.start()
        self.addCleanup(modules.stop)
        output = redirect_stdout(io.StringIO())
        output.__enter__()
        self.addCleanup(output.__exit__, None, None, None)

    def test_open_path_with_spaces(self):
        """Acepta un nombre con espacios y cierra la ventana oculta del selector."""
        source = Path("carpeta con espacios") / "entrada.txt"
        self.native.askopenfilename.return_value = str(source)
        self.assertEqual(
            dialogs.choose_path("Abrir", filetypes=dialogs.TEXT_FILES), source
        )
        self.root.withdraw.assert_called_once()
        self.root.destroy.assert_called_once()
        self.assertEqual(
            self.native.askopenfilename.call_args.kwargs["filetypes"],
            dialogs.TEXT_FILES,
        )

    def test_save_uses_proposed_name_directory_and_extension(self):
        """Transmite nombre, carpeta y extensión sugeridos al selector de guardado."""
        target = Path.cwd() / "resultado.txt"
        self.native.asksaveasfilename.return_value = str(target)
        self.assertEqual(
            dialogs.choose_path("Guardar", save=True, default=target, extension=".txt"),
            target,
        )
        options = self.native.asksaveasfilename.call_args.kwargs
        self.assertEqual(options["initialfile"], target.name)
        self.assertEqual(Path(options["initialdir"]), target.parent)
        self.assertEqual(options["defaultextension"], ".txt")
        self.root.destroy.assert_called_once()

    def test_cancel_open_and_save_close_window(self):
        """Cierra la ventana oculta al cancelar tanto la apertura como el guardado."""
        for save in (False, True):
            with self.subTest(save=save):
                self.root.reset_mock()
                with self.assertRaises(OperationCancelled):
                    dialogs.choose_path("Seleccionar", save=save)
                self.root.destroy.assert_called_once()

    def test_no_desktop_falls_back_to_terminal(self):
        """Solicita una ruta por terminal cuando no se puede crear una ventana Tk."""
        self.tk.Tk.side_effect = self.tk.TclError("sin escritorio")
        with patch("builtins.input", return_value='"carpeta con espacios/entrada.txt"'):
            self.assertEqual(
                dialogs.choose_path("Abrir"), Path("carpeta con espacios/entrada.txt")
            )

    def test_dialog_failure_closes_window_before_fallback(self):
        """Destruye la ventana antes de pedir una ruta por terminal tras un fallo."""
        self.native.askopenfilename.side_effect = self.tk.TclError("sin escritorio")

        def enter_path(prompt):
            """Exige que la ventana esté cerrada antes de devolver una ruta de terminal."""
            self.root.destroy.assert_called_once()
            return "entrada.txt"

        with patch("builtins.input", side_effect=enter_path):
            self.assertEqual(dialogs.choose_path("Abrir"), Path("entrada.txt"))

    def test_missing_tkinter_fallback_expands_home_and_adds_extension(self):
        """Sin Tkinter, expande ~ y completa la extensión desde la entrada por terminal."""
        with patch.dict(sys.modules, {"tkinter": None}):
            with patch("builtins.input", return_value="~/resultado"):
                self.assertEqual(
                    dialogs.choose_path("Guardar", save=True, extension=".txt"),
                    Path("~/resultado.txt").expanduser(),
                )

    def test_empty_terminal_path_cancels(self):
        """Trata las rutas vacías, incluso entre comillas, como una cancelación."""
        with patch.dict(sys.modules, {"tkinter": None}):
            for value in ("", "  ", '""', "''"):
                with (
                    self.subTest(value=value),
                    patch("builtins.input", return_value=value),
                ):
                    with self.assertRaises(OperationCancelled):
                        dialogs.choose_path("Abrir")
