"""Errores que la interfaz puede comunicar sin mostrar un traceback."""


class EnigmaError(Exception):
    """Error controlado de la aplicación."""


class ValidationError(EnigmaError):
    """Entrada no admitida por la operación."""


class OperationCancelled(EnigmaError):
    """El usuario ha cancelado la operación."""
