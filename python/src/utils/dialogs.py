"""Selectores nativos para abrir archivos y elegir destinos de guardado."""

from contextlib import suppress
from pathlib import Path

from ..errors import OperationCancelled

ALL_FILES = (("Todos los archivos", "*"),)
TEXT_FILES = (("Archivos de texto", "*.txt"),) + ALL_FILES
ENCRYPTED_FILES = (("Archivos cifrados", "*.enc"),) + ALL_FILES


def _prompt_path(
    title: str, *, save: bool, default: Path | None, extension: str
) -> Path:
    print(f"{title}: selector de archivos en la terminal (sin escritorio disponible).")
    hint = f" [{default}]" if save and default is not None else ""
    value = input(f"Ruta{hint} (vacío para cancelar): ").strip()
    if not value:
        raise OperationCancelled
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        value = value[1:-1]
    if not value:
        raise OperationCancelled
    path = Path(value).expanduser()
    if save and extension and not path.suffix:
        path = path.with_suffix(extension)
    return path


def choose_path(
    title: str,
    *,
    save: bool = False,
    default: Path | None = None,
    filetypes: tuple = ALL_FILES,
    extension: str = "",
) -> Path:
    try:
        import tkinter as tk
        from tkinter import filedialog
    except ImportError:
        return _prompt_path(title, save=save, default=default, extension=extension)

    root = None
    try:
        root = tk.Tk()
        root.withdraw()
        root.attributes("-topmost", True)
        options = {
            "parent": root,
            "title": title,
            "filetypes": filetypes,
            "initialdir": str(
                default.absolute().parent if default is not None else Path.cwd()
            ),
        }
        if save:
            if default is not None:
                options["initialfile"] = default.name
            # La capa de archivos sigue rechazando cualquier sobrescritura.
            value = filedialog.asksaveasfilename(defaultextension=extension, **options)
        else:
            value = filedialog.askopenfilename(**options)
    except tk.TclError:
        value = None
    finally:
        if root is not None:
            with suppress(tk.TclError):
                root.destroy()

    if value is None:
        return _prompt_path(title, save=save, default=default, extension=extension)
    if not value:
        raise OperationCancelled
    return Path(value)
