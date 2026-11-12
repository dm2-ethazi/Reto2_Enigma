"""Muestra el resultado en el explorador sin ejecutar el archivo generado."""

import subprocess
import sys
from pathlib import Path

from ..i18n import tr


def reveal_file(path: str | Path) -> bool:
    """Intenta mostrar el resultado en el explorador de la plataforma.

    Args:
        path: Ruta del archivo ya guardado, que se convierte a absoluta.

    Returns:
        True si el proceso se inicia; False si no se puede iniciar. No espera a
        que termine el explorador ni comprueba su código de salida.

    Windows y macOS seleccionan el archivo; Linux abre su carpeta. No ejecuta el
    archivo ni utiliza un shell. Si el lanzamiento falla, muestra la ruta.
    """
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
