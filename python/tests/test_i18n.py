"""Contratos de traducción y flujos completos para los tres idiomas."""

import ast
import errno
import io
import json
import sys
from contextlib import redirect_stdout, nullcontext
from pathlib import Path
from string import Formatter
from unittest.mock import MagicMock, patch

import pytest

import main
from src import i18n
from src.encrypt import aes, vigenere
from src.errors import OperationCancelled, ValidationError
from src.interface import app, layout, menu, navigation
from src.interface import aes as aes_screen
from src.interface import vigenere as vigenere_screen
from src.interface.loading import loading
from src.utils import dialogs, explorer, files


@pytest.fixture(autouse=True)
def reset_language():
    i18n.set_language("es")
    yield
    i18n.set_language("es")


@pytest.mark.parametrize("language", ["en", "eu"])
def test_catalogues_cover_source_messages_and_preserve_placeholders(language):
    root = Path(i18n.__file__).parent
    catalogues = {
        lang: json.loads(
            (root / "locales" / f"{lang}.json").read_text(encoding="utf-8")
        )
        for lang in ("en", "eu")
    }
    assert catalogues["en"].keys() == catalogues["eu"].keys()
    catalogue = catalogues[language]
    for path in root.rglob("*.py"):
        for node in ast.walk(ast.parse(path.read_text(encoding="utf-8"))):
            if (
                isinstance(node, ast.Call)
                and isinstance(node.func, ast.Name)
                and node.func.id == "tr"
                and node.args
                and isinstance(node.args[0], ast.Constant)
            ):
                assert node.args[0].value in catalogue, (path, node.args[0].value)
    for source, translation in catalogue.items():
        assert translation.strip()
        assert {name for _, name, _, _ in Formatter().parse(source) if name} == {
            name for _, name, _, _ in Formatter().parse(translation) if name
        }
    assert all(label in catalogue for label in menu.CHOICES)


def test_language_validation_fallback_and_user_data():
    assert i18n.get_language() == "es"
    with pytest.raises(ValueError):
        i18n.set_language("fr")
    assert i18n.get_language() == "es"
    for language in i18n.LANGUAGES:
        i18n.set_language(language)
        assert i18n.tr("unknown {value}", value="{ñ}") == "unknown {ñ}"
        assert i18n.tr("Texto con {llaves}") == "Texto con {llaves}"
        assert "{ñ}" in i18n.tr("Archivo cifrado: {path}", path="{ñ}")


@pytest.mark.parametrize("language", i18n.LANGUAGES)
@pytest.mark.parametrize("interactive", [False, True])
def test_menu_and_keyboard_help_use_current_language(language, interactive):
    i18n.set_language(language)
    output = layout.render("Menú principal", menu.CHOICES, 0, (), interactive)
    for source in ("Menú principal", "Cifrar Vigenère", "AES / ARCHIVOS", "Salir"):
        assert i18n.tr(source) in output
    assert "5. " + i18n.tr("Salir") in output
    assert "6. " in output
    if interactive:
        # El texto se puede dividir al ajustar al ancho de la terminal.
        assert (
            i18n.tr("Flechas o j/k: mover | Enter: elegir | Esc: volver").split()[0]
            in output
        )


def test_change_language_repaints_menu_and_can_be_cancelled():
    with (
        patch.object(app, "enable_cursor_control", return_value=False),
        patch("builtins.input", side_effect=["6", "2", "6", "3", "6", "q", "5"]),
        redirect_stdout(io.StringIO()) as output,
    ):
        assert app.run() == 0
    assert i18n.get_language() == "en"
    for title in ("Menú principal", "Menu nagusia", "Main menu", "Goodbye."):
        assert title in output.getvalue()


@pytest.mark.parametrize("language", i18n.LANGUAGES)
def test_cli_language(language):
    with patch.object(main, "run", return_value=0):
        assert main.main(["--lang", language]) == 0
    assert i18n.get_language() == language


def test_cli_defaults_to_spanish_and_rejects_unknown_language():
    i18n.set_language("en")
    with patch.object(main, "run", return_value=0) as run:
        assert main.main([]) == 0
        assert i18n.get_language() == "es"
        with pytest.raises(SystemExit) as error:
            main.main(["--lang", "fr"])
        assert error.value.code == 2
        assert run.call_count == 1


@pytest.mark.parametrize("language", i18n.LANGUAGES)
@pytest.mark.parametrize("decrypt", [False, True])
def test_vigenere_console_flow_preserves_user_text(language, decrypt):
    i18n.set_language(language)
    original = "ATTACKATDAWN ñ {Texto: }"
    encrypted = vigenere.encrypt(original, "LEMON")
    with (
        patch.object(app, "enable_cursor_control", return_value=False),
        patch.object(vigenere_screen.sys.stdin, "isatty", return_value=False),
        patch(
            "builtins.input",
            side_effect=[
                "2" if decrypt else "1",
                "1",
                encrypted if decrypt else original,
                "LEMON",
                "1",
                "",
                "5",
            ],
        ) as prompt,
        redirect_stdout(io.StringIO()) as output,
    ):
        assert app.run() == 0
    expected = original if decrypt else encrypted
    assert i18n.tr("\nResultado:\n{result}", result=expected) in output.getvalue()
    assert i18n.tr("Hasta pronto.") in output.getvalue()
    assert any(
        call.args == (i18n.tr("Clave Vigenère (1-64 letras A-Z): "),)
        for call in prompt.call_args_list
    )


@pytest.mark.parametrize("language", i18n.LANGUAGES)
def test_aes_roundtrip_dialog_titles_password_and_authentication(language, tmp_path):
    i18n.set_language(language)
    original = tmp_path / "original.bin"
    encrypted = tmp_path / "secret.enc"
    restored = tmp_path / "restored.bin"
    original.write_bytes(bytes(range(256)))
    with (
        patch.object(
            aes_screen, "choose_path", side_effect=[original, encrypted]
        ) as picker,
        patch.object(aes_screen, "reveal_file"),
        patch("builtins.input", return_value=""),
        redirect_stdout(io.StringIO()) as output,
    ):
        aes_screen.run_encryption()
    assert picker.call_args_list[0].args == (
        i18n.tr("Seleccionar archivo para cifrar con AES"),
    )
    prefix = i18n.tr("Contraseña generada: {password}", password="")
    password = next(
        line[len(prefix) :]
        for line in output.getvalue().splitlines()
        if line.startswith(prefix)
    )
    assert len(password) == 16
    with (
        patch.object(aes_screen, "choose_path", side_effect=[encrypted, restored]),
        patch.object(aes_screen, "read_password", return_value=password),
        patch.object(aes_screen, "reveal_file"),
        patch("builtins.input", return_value=""),
        redirect_stdout(io.StringIO()) as output,
    ):
        aes_screen.run_decryption()
    assert original.read_bytes() == restored.read_bytes()
    assert (
        i18n.tr("Archivo descifrado y autenticado: {path}", path=restored)
        in output.getvalue()
    )
    with pytest.raises(ValidationError) as error:
        aes.decrypt_file(encrypted, "A" * 16, tmp_path / "invalid.bin")
    assert str(error.value) == i18n.tr(
        "No se pudo autenticar el archivo: contraseña incorrecta o contenido/cabecera AAD alterados."
    )
    assert not (tmp_path / "invalid.bin").exists()


@pytest.mark.parametrize("language", i18n.LANGUAGES)
def test_validation_dialog_filters_and_terminal_fallback(language):
    i18n.set_language(language)
    with pytest.raises(ValidationError) as error:
        vigenere.validate_key("ñ")
    assert str(error.value) == i18n.tr(
        "La clave Vigenère debe contener entre 1 y 64 letras A-Z."
    )
    tk = MagicMock()
    tk.TclError = type("TclError", (Exception,), {})
    tk.filedialog.askopenfilename.return_value = "input.txt"
    with patch.dict(sys.modules, {"tkinter": tk, "tkinter.filedialog": tk.filedialog}):
        assert dialogs.choose_path(
            "Seleccionar archivo de texto", filetypes=dialogs.TEXT_FILES
        ) == Path("input.txt")
    options = tk.filedialog.askopenfilename.call_args.kwargs
    assert options["title"] == i18n.tr("Seleccionar archivo de texto")
    assert options["filetypes"][0] == (i18n.tr("Archivos de texto"), "*.txt")
    with (
        patch.dict(sys.modules, {"tkinter": None}),
        patch("builtins.input", return_value="") as prompt,
        redirect_stdout(io.StringIO()) as output,
        pytest.raises(OperationCancelled),
    ):
        dialogs.choose_path("Seleccionar archivo de texto")
    assert i18n.tr("Seleccionar archivo de texto") in output.getvalue()
    assert prompt.call_args.args == (
        i18n.tr("Ruta{hint} (vacío para cancelar): ", hint=""),
    )


@pytest.mark.parametrize("language", i18n.LANGUAGES)
def test_loading_and_file_manager_failure(language):
    i18n.set_language(language)
    with (
        redirect_stdout(io.StringIO()) as output,
        patch.object(explorer.subprocess, "Popen", side_effect=OSError),
    ):
        with loading():
            pass
        assert not explorer.reveal_file("result.txt")
    assert i18n.tr("Cifrando") + "..." in output.getvalue()
    assert (
        i18n.tr(
            "No se pudo abrir el explorador. Archivo guardado en: {path}",
            path=Path("result.txt").absolute(),
        )
        in output.getvalue()
    )


@pytest.mark.parametrize(
    "word", ["q", "salir", "volver", "exit", "back", "irten", "atzera"]
)
def test_navigation_accepts_exit_words(word):
    with patch("builtins.input", return_value=word), redirect_stdout(io.StringIO()):
        assert navigation.choose("test", ["test"]) is None


def test_interactive_language_switch():
    with (
        patch.object(navigation, "keyboard", side_effect=nullcontext),
        patch.object(navigation, "read_key", side_effect=["end", "enter"]),
        redirect_stdout(io.StringIO()),
    ):
        menu.change_language(interactive=True)
    assert i18n.get_language() == "en"


@pytest.mark.parametrize("language", ["en", "eu"])
@pytest.mark.parametrize(
    "error, message",
    [
        (
            FileNotFoundError(errno.ENOENT, "system message", "missing.txt"),
            "El archivo o carpeta no existe.",
        ),
        (
            PermissionError(errno.EACCES, "system message", "private.txt"),
            "Permiso denegado para acceder al archivo o carpeta.",
        ),
        (
            OSError(errno.EIO, "system message"),
            "No se pudo acceder al archivo o carpeta.",
        ),
        (
            UnicodeError("system message"),
            "El archivo de texto no contiene UTF-8 válido.",
        ),
        (MemoryError(), "Recursos insuficientes para completar la operación."),
    ],
)
def test_system_errors_use_session_language(language, error, message):
    i18n.set_language(language)
    with (
        patch.object(app, "enable_cursor_control", return_value=False),
        patch.object(app, "run_encryption", side_effect=error),
        patch("builtins.input", side_effect=["1", "", "5"]),
        redirect_stdout(io.StringIO()) as output,
    ):
        assert app.run() == 0
    assert i18n.tr(message) in output.getvalue()
    assert "system message" not in output.getvalue()


def test_existing_destination_message_is_translated_without_changing_path(tmp_path):
    i18n.set_language("eu")
    target = tmp_path / "Menú principal {name}.txt"
    target.touch()
    with pytest.raises(FileExistsError) as error:
        files.new_destination(target)
    assert str(error.value) == i18n.tr(
        "El destino ya existe: {destination}. Elige otro nombre.", destination=target
    )
