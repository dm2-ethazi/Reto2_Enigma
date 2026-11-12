"""Selectores nativos para abrir archivos y elegir destinos de guardado."""

from contextlib import suppress
from pathlib import Path

from ..errors import OperationCancelled
from ..i18n import tr

ALL_FILES = (("Todos los archivos", "*"),)
TEXT_FILES = (("Archivos de texto", "*.txt"),) + ALL_FILES
ENCRYPTED_FILES = (("Archivos cifrados", "*.enc"),) + ALL_FILES


def _prompt_path(
    title: str, *, save: bool, default: Path | None, extension: str
) -> Path:
    """Solicita una ruta por terminal cuando no hay selector gráfico disponible.

    Muestra default como sugerencia al guardar, elimina comillas exteriores,
    expande ~ y añade extension si falta. Una respuesta vacía cancela mediante
    OperationCancelled; no utiliza automáticamente la sugerencia.
    """
    print(
        tr(
            "{title}: selector de archivos en la terminal (sin escritorio disponible).",
            title=title,
        )
    )
    hint = f" [{default}]" if save and default is not None else ""
    value = input(tr("Ruta{hint} (vacío para cancelar): ", hint=hint)).strip()
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
    """Solicita una ruta con Tkinter o recurre a la entrada por terminal.

    Args:
        title: Título o clave de traducción del selector.
        save: Abre un selector de guardado cuando es True.
        default: Ruta sugerida para la carpeta inicial y el nombre de guardado.
        filetypes: Pares de etiqueta y patrón para los filtros de archivos.
        extension: Extensión propuesta al guardar, por ejemplo .txt.

    Returns:
        Ruta seleccionada; la capa de archivos valida origen y destino después.

    Raises:
        OperationCancelled: Si se cancela el selector o la entrada por terminal.

    Traduce títulos y etiquetas al mostrar el diálogo. La ventana oculta siempre
    se destruye; un fallo de Tkinter permite continuar desde la terminal.
    """
    title = tr(title)
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
            "filetypes": tuple((tr(label), pattern) for label, pattern in filetypes),
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

    # None indica un fallo gráfico; la cadena vacía indica cancelación explícita.
    if value is None:
        return _prompt_path(title, save=save, default=default, extension=extension)
    if not value:
        raise OperationCancelled
    return Path(value)
