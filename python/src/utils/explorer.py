"""Muestra el resultado en el explorador sin ejecutar el archivo generado."""

import subprocess
import sys
from pathlib import Path

from ..i18n import tr


def reveal_file(path: str | Path) -> bool:
    path = Path(path).absolute()
    if sys.platform == "win32":
        command = ["explorer.exe", "/select,", str(path)]
    elif sys.platform == "darwin":
        command = ["open", "-R", str(path)]
    else:
        command = ["xdg-open", str(path.parent)]
    try:
        subprocess.Popen(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except OSError:
        print(
            tr("No se pudo abrir el explorador. Archivo guardado en: {path}", path=path)
        )
        return False
    return True
