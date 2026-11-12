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
from ..i18n import tr

MAX_PART_SIZE = 20 * 1024 * 1024
COPY_CHUNK_SIZE = 1024 * 1024


def input_file(path: str | Path) -> Path:
    """Expande ~ y devuelve la ruta si corresponde a un archivo regular.

    Raises:
        ValidationError: Si la ruta corresponde a un directorio u otro tipo.
        OSError: Si el origen no existe o no se puede consultar.
    """
    source = Path(path).expanduser()
    if not stat.S_ISREG(source.stat().st_mode):
        raise ValidationError(tr("El origen debe ser un archivo regular."))
    return source


def new_destination(path: str | Path) -> Path:
    """Devuelve una ruta absoluta nueva con carpeta padre existente.

    Raises:
        FileExistsError: Si ya existe la ruta, incluido un enlace simbólico roto.
        ValidationError: Si la carpeta padre no existe.

    Esta comprobación no reserva el nombre. La publicación debe comprobar de
    nuevo que el destino sigue libre para evitar sobrescrituras por carreras.
    """
    destination = Path(path).expanduser().absolute()
    # lexists también detecta enlaces simbólicos cuyo destino ya no existe.
    if os.path.lexists(destination):
        raise FileExistsError(
            tr(
                "El destino ya existe: {destination}. Elige otro nombre.",
                destination=destination,
            )
        )
    if not destination.parent.is_dir():
        raise ValidationError(tr("La carpeta de destino no existe."))
    return destination


class _PartWriter:
    """Acumula bytes en temporales de hasta MAX_PART_SIZE, en orden.

    Los archivos se crean al escribir; finish crea una parte vacía si no hay
    datos. Cerrar la escritura no elimina las partes: cleanup es quien las
    retira, normalmente al salir del contexto temporary_parts.
    """

    def __init__(self, directory: Path):
        """Inicializa el escritor en directory sin crear todavía archivos temporales."""
        self.directory = directory
        self.parts: list[Path] = []
        self._stream: BinaryIO | None = None
        self._size = 0
        self._closed = False

    def _open_part(self) -> None:
        """Crea una parte exclusiva y asume la propiedad de su descriptor binario."""
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
        """Vacía, sincroniza y cierra la parte activa, si existe."""
        if self._stream is not None:
            stream, self._stream = self._stream, None
            with stream:
                stream.flush()
                os.fsync(stream.fileno())

    def write(self, data: bytes) -> int:
        """Añade data repartiendo los bytes entre partes de tamaño limitado.

        Returns:
            Número total de bytes escritos.

        Raises:
            ValueError: Si finish o cleanup ya cerraron el escritor.
            OSError: Si no se puede escribir o sincronizar una parte.
        """
        if self._closed:
            raise ValueError(tr("El escritor de partes está cerrado."))
        remaining = memoryview(data).cast("B")
        total = len(remaining)
        while remaining:
            if self._stream is None:
                self._open_part()
            count = min(len(remaining), MAX_PART_SIZE - self._size)
            written = self._stream.write(remaining[:count])
            if written is None or written <= 0:
                raise OSError(tr("No se pudo completar la escritura del temporal."))
            self._size += written
            # Una escritura puede aceptar menos bytes de los solicitados.
            remaining = remaining[written:]
            if self._size == MAX_PART_SIZE:
                self._finish_part()
        return total

    def finish(self) -> None:
        """Cierra la escritura de forma repetible y asegura al menos una parte."""
        if not self.parts:
            # Un archivo vacío también necesita una parte que se pueda publicar.
            self._open_part()
        self._finish_part()
        self._closed = True

    def publish(self, path: str | Path) -> Path:
        """Finaliza las partes y las publica en un destino nuevo; devuelve su ruta.

        Propaga errores de validación, colisiones y acceso al disco. Las partes se
        conservan hasta cleanup, incluso tras publicar, para poder leerlas después.
        """
        self.finish()
        destination = new_destination(path)
        _publish_parts(self.parts, destination)
        return destination

    def iter_text(self) -> Iterator[str]:
        """Finaliza y genera texto UTF-8 leyendo las partes en orden.

        Mantiene un decodificador incremental entre lecturas y partes para aceptar
        caracteres divididos. Propaga UnicodeDecodeError si el contenido es inválido.
        """
        self.finish()
        decoder = codecs.getincrementaldecoder("utf-8")()
        for part in self.parts:
            with part.open("rb") as source:
                while block := source.read(COPY_CHUNK_SIZE):
                    yield decoder.decode(block)
        yield decoder.decode(b"", final=True)

    def cleanup(self) -> None:
        """Cierra el flujo y trata de borrar todas las partes sin ocultar otros errores."""
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
    """Publica partes ordenadas sin reemplazar un destino existente.

    Una sola parte utiliza un enlace duro atómico. Varias se copian a un archivo
    creado en exclusiva, visible durante la unión. Si esta falla, se intenta
    borrar el destino incompleto y se propaga el error original.
    """
    if len(parts) == 1:
        # El enlace falla si el destino aparece tras la validación previa.
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
    """Cede un escritor de partes y limpia sus temporales al salir.

    Args:
        directory: Carpeta existente para las partes; None usa la temporal del
            sistema. No crea carpetas ni publica automáticamente el resultado.

    Yields:
        Escritor que permite escribir, mostrar o publicar las partes.

    La limpieza también se ejecuta si el bloque falla o se interrumpe.
    """
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
    """Valida un origen regular con extensión .txt, sin distinguir su caja.

    No lee ni valida el contenido UTF-8; esa tarea corresponde al consumidor.
    Propaga los errores de input_file y ValidationError por extensión incorrecta.
    """
    source = input_file(path)
    if source.suffix.lower() != ".txt":
        raise ValidationError(tr("Vigenère requiere un archivo .txt en UTF-8."))
    return source


def new_text_destination(path: str | Path) -> Path:
    """Exige extensión .txt y aplica las garantías de new_destination."""
    destination = Path(path).expanduser()
    if destination.suffix.lower() != ".txt":
        raise ValidationError(tr("El resultado Vigenère debe guardarse como .txt."))
    return new_destination(destination)


def read_text(path: str | Path) -> str:
    # No normalizar CRLF ni eliminar BOM: se preserva todo el texto original.
    """Lee un .txt completo como UTF-8, conservando BOM y saltos de línea.

    Propaga errores de acceso, validación de ruta y UnicodeDecodeError. Para
    archivos grandes se prefieren las funciones por bloques de Vigenère.
    """
    return input_text_file(path).read_bytes().decode("utf-8")


def write_text(path: str | Path, text: str) -> Path:
    """Guarda texto UTF-8 en un .txt nuevo y devuelve la ruta expandida.

    Codifica fragmentos sin normalizar el contenido y publica mediante
    staged_output. Propaga errores de ruta, codificación y escritura; nunca
    reemplaza un archivo de destino existente.
    """
    destination = new_text_destination(path)
    with staged_output(destination) as stream:
        for offset in range(0, len(text), COPY_CHUNK_SIZE):
            stream.write(text[offset : offset + COPY_CHUNK_SIZE].encode("utf-8"))
    return Path(path).expanduser()
