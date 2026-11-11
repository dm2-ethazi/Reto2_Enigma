"""Lectura y publicación de archivos nuevos sin reemplazar destinos existentes."""

import codecs
import os
import stat
import tempfile
from collections.abc import Iterator
from contextlib import contextmanager, suppress
from pathlib import Path
from typing import BinaryIO

from ..errors import ValidationError

MAX_PART_SIZE = 20 * 1024 * 1024
COPY_CHUNK_SIZE = 1024 * 1024


def input_file(path: str | Path) -> Path:
    source = Path(path).expanduser()
    if not stat.S_ISREG(source.stat().st_mode):
        raise ValidationError("El origen debe ser un archivo regular.")
    return source


def new_destination(path: str | Path) -> Path:
    destination = Path(path).expanduser().absolute()
    # lexists también detecta enlaces simbólicos cuyo destino ya no existe.
    if os.path.lexists(destination):
        raise FileExistsError(
            f"El destino ya existe: {destination}. Elige otro nombre."
        )
    if not destination.parent.is_dir():
        raise ValidationError("La carpeta de destino no existe.")
    return destination


class _PartWriter:
    """Escribe secuencialmente sin superar 20 MiB en ningún temporal."""

    def __init__(self, directory: Path):
        self.directory = directory
        self.parts: list[Path] = []
        self._stream: BinaryIO | None = None
        self._size = 0
        self._closed = False

    def _open_part(self) -> None:
        descriptor, name = tempfile.mkstemp(
            prefix=".enigma-", suffix=".part", dir=self.directory
        )
        self.parts.append(Path(name))
        try:
            self._stream = os.fdopen(descriptor, "wb")
        except BaseException:
            os.close(descriptor)
            raise
        self._size = 0

    def _finish_part(self) -> None:
        if self._stream is not None:
            stream, self._stream = self._stream, None
            with stream:
                stream.flush()
                os.fsync(stream.fileno())

    def write(self, data: bytes) -> int:
        if self._closed:
            raise ValueError("El escritor de partes está cerrado.")
        remaining = memoryview(data).cast("B")
        total = len(remaining)
        while remaining:
            if self._stream is None:
                self._open_part()
            count = min(len(remaining), MAX_PART_SIZE - self._size)
            written = self._stream.write(remaining[:count])
            if written is None or written <= 0:
                raise OSError("No se pudo completar la escritura del temporal.")
            self._size += written
            remaining = remaining[written:]
            if self._size == MAX_PART_SIZE:
                self._finish_part()
        return total

    def finish(self) -> None:
        if not self.parts:
            self._open_part()
        self._finish_part()
        self._closed = True

    def publish(self, path: str | Path) -> Path:
        self.finish()
        destination = new_destination(path)
        _publish_parts(self.parts, destination)
        return destination

    def iter_text(self) -> Iterator[str]:
        """Lee el resultado por bloques, incluso si una parte divide un carácter."""
        self.finish()
        decoder = codecs.getincrementaldecoder("utf-8")()
        for part in self.parts:
            with part.open("rb") as source:
                while block := source.read(COPY_CHUNK_SIZE):
                    yield decoder.decode(block)
        yield decoder.decode(b"", final=True)

    def cleanup(self) -> None:
        self._closed = True
        try:
            if self._stream is not None:
                with suppress(OSError):
                    self._stream.close()
        finally:
            self._stream = None
            for part in self.parts:
                # No ocultar el error original si el sistema impide la limpieza.
                with suppress(OSError):
                    part.unlink()


def _publish_parts(parts: list[Path], destination: Path) -> None:
    if len(parts) == 1:
        os.link(parts[0], destination)
        return

    # Crear en exclusiva: nunca borrar ni reemplazar un destino preexistente.
    stream = destination.open("xb")
    try:
        with stream:
            for part in parts:
                with part.open("rb") as source:
                    while block := source.read(COPY_CHUNK_SIZE):
                        stream.write(block)
            stream.flush()
            os.fsync(stream.fileno())
    except BaseException:
        with suppress(OSError):
            destination.unlink()
        raise


@contextmanager
def temporary_parts(directory: str | Path | None = None) -> Iterator[_PartWriter]:
    """Mantiene el resultado en partes hasta mostrarlo o publicarlo."""
    folder = Path(directory) if directory is not None else Path(tempfile.gettempdir())
    writer = _PartWriter(folder)
    try:
        yield writer
    finally:
        writer.cleanup()


@contextmanager
def staged_output(path: str | Path) -> Iterator[_PartWriter]:
    """Prepara partes de hasta 20 MiB y guarda un único archivo sin sobrescribir.

    Una parte se publica atómicamente mediante enlace duro. Varias partes se
    unen directamente en un destino nuevo, visible durante la unión; un error
    controlado elimina ese destino incompleto. No se crea otro temporal grande.
    """
    destination = new_destination(path)
    with temporary_parts(destination.parent) as writer:
        yield writer
        writer.publish(destination)


def input_text_file(path: str | Path) -> Path:
    source = input_file(path)
    if source.suffix.lower() != ".txt":
        raise ValidationError("Vigenère requiere un archivo .txt en UTF-8.")
    return source


def new_text_destination(path: str | Path) -> Path:
    destination = Path(path).expanduser()
    if destination.suffix.lower() != ".txt":
        raise ValidationError("El resultado Vigenère debe guardarse como .txt.")
    return new_destination(destination)


def read_text(path: str | Path) -> str:
    # No normalizar CRLF ni eliminar BOM: se preserva todo el texto original.
    return input_text_file(path).read_bytes().decode("utf-8")


def write_text(path: str | Path, text: str) -> Path:
    destination = new_text_destination(path)
    with staged_output(destination) as stream:
        for offset in range(0, len(text), COPY_CHUNK_SIZE):
            stream.write(text[offset : offset + COPY_CHUNK_SIZE].encode("utf-8"))
    return Path(path).expanduser()
