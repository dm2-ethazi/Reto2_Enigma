"""Configuración de registros locales con un archivo por día natural."""

import logging
from datetime import datetime
from pathlib import Path

_LOGGER_NAME = "enigma"


class DailyFileHandler(logging.Handler):
    """Añade cada registro a un archivo UTF-8 nombrado con la fecha local."""

    def __init__(self, directory: Path) -> None:
        """Prepara el directorio local en el que se crearán los archivos diarios."""
        super().__init__()
        self.directory = directory
        self.directory.mkdir(parents=True, exist_ok=True)

    def emit(self, record: logging.LogRecord) -> None:
        """Escribe un registro en el archivo correspondiente al día local."""
        try:
            local_day = datetime.fromtimestamp(record.created).astimezone().date()
            path = self.directory / f"enigma-{local_day:%Y-%m-%d}.log"
            with path.open("a", encoding="utf-8") as output:
                output.write(f"{self.format(record)}\n")
        except Exception:
            self.handleError(record)


def configure_logging(directory: Path | None = None) -> logging.Logger:
    """Configura una vez el registrador local y lo devuelve.

    Args:
        directory: Carpeta opcional de registros. Por defecto, python/logs.

    Returns:
        Registrador enigma a nivel INFO, sin mensajes en la consola.

    Si el proceso continúa después de medianoche, cada evento se dirige al
    archivo del día local que le corresponda. Las sesiones del mismo día
    comparten el archivo y añaden entradas al final.
    """
    logger = logging.getLogger(_LOGGER_NAME)
    logger.setLevel(logging.INFO)
    logger.propagate = False
    if not any(isinstance(handler, DailyFileHandler) for handler in logger.handlers):
        log_directory = directory or Path(__file__).resolve().parents[1] / "logs"
        handler = DailyFileHandler(log_directory)
        handler.setFormatter(
            logging.Formatter("%(asctime)s %(levelname)s %(name)s: %(message)s")
        )
        logger.addHandler(handler)
    return logger
