"""Vigenère educativo: solo A-Z avanza la clave; se conserva el resto del texto."""

import codecs
from collections.abc import Iterable, Iterator
from pathlib import Path

from ..errors import ValidationError
from ..i18n import tr
from ..utils.files import input_text_file

FILE_BLOCK_SIZE = 1024 * 1024


def validate_key(key: str) -> str:
    """Valida entre 1 y 64 letras ASCII y devuelve la clave en mayúsculas.

    Raises:
        ValidationError: Si la clave está vacía, es demasiado larga o contiene
            caracteres fuera de A-Z y a-z.
    """
    if not 1 <= len(key) <= 64 or not all(
        "A" <= c <= "Z" or "a" <= c <= "z" for c in key
    ):
        raise ValidationError(
            tr("La clave Vigenère debe contener entre 1 y 64 letras A-Z.")
        )
    return key.upper()


def encrypt(text: str, key: str) -> str:
    """Devuelve el texto cifrado con una clave válida de 1 a 64 letras A-Z.

    Solo transforma letras ASCII y conserva su caja. El resto de caracteres no
    avanza la clave. Una clave no válida produce ValidationError.
    """
    return _transform(text, key, 1)


def decrypt(text: str, key: str) -> str:
    """Invierte Vigenère con la misma clave, conservando el resto del texto.

    Valida la clave como encrypt. Este cifrado educativo no autentica los datos:
    una clave válida pero equivocada genera otro texto sin producir un error.
    """
    return _transform(text, key, -1)


def _transform(text: str, key: str, direction: int) -> str:
    """Aplica desplazamientos de clave: direction=1 cifra y direction=-1 descifra."""
    key = validate_key(key)
    result = []
    index = 0
    for character in text:
        if "A" <= character <= "Z":
            base = ord("A")
        elif "a" <= character <= "z":
            base = ord("a")
        else:
            result.append(character)
            # Los signos y las letras no ASCII se conservan sin consumir clave.
            continue
        shift = ord(key[index % len(key)]) - ord("A")
        result.append(chr(base + (ord(character) - base + direction * shift) % 26))
        index += 1
    return "".join(result)


def encrypt_chunks(chunks: Iterable[bytes], key: str) -> Iterator[bytes]:
    """Genera bloques cifrados sin reiniciar la clave entre bloques.

    Args:
        chunks: Iterable de bloques de bytes que, juntos, forman texto UTF-8.
        key: Clave de 1 a 64 letras ASCII.

    Yields:
        Bloques de bytes del mismo tamaño que los recibidos, conservando BOM,
        saltos de línea y caracteres fuera de A-Z/a-z.

    Raises:
        ValidationError: Si la clave no es válida, incluso con entrada vacía.
        UnicodeDecodeError: Si la secuencia UTF-8 es inválida o queda truncada.

    La validación es incremental: pueden haberse generado bloques antes de
    detectar un error. El consumidor debe publicarlos solo al finalizar.
    """
    yield from _transform_chunks(chunks, key, 1)


def decrypt_chunks(chunks: Iterable[bytes], key: str) -> Iterator[bytes]:
    """Genera bloques descifrados manteniendo la posición de la clave.

    Comparte el contrato de encrypt_chunks: valida la clave y el UTF-8 durante
    la iteración, y conserva el tamaño de los bloques y los bytes no ASCII.
    """
    yield from _transform_chunks(chunks, key, -1)


def _transform_chunks(chunks, key, direction):
    """Transforma bytes ASCII y valida UTF-8 sin recomponer el archivo.

    El decodificador incremental acepta caracteres divididos entre bloques;
    position mantiene la clave y direction determina cifrado o descifrado.
    """
    shifts = [ord(letter) - ord("A") for letter in validate_key(key)]
    key_length = len(shifts)
    position = 0
    decoder = codecs.getincrementaldecoder("utf-8")()
    for block in chunks:
        # Validar UTF-8 incrementalmente; BOM, CRLF y bytes no ASCII se conservan.
        decoder.decode(block)
        result = bytearray(block)
        for index, character in enumerate(block):
            if 65 <= character <= 90:
                base = 65
            elif 97 <= character <= 122:
                base = 97
            else:
                continue
            result[index] = (
                base + (character - base + direction * shifts[position]) % 26
            )
            position = (position + 1) % key_length
        yield bytes(result)
    # Detectar también un carácter multibyte incompleto al terminar el iterable.
    decoder.decode(b"", final=True)


def encrypt_file(path: str | Path, key: str) -> Iterator[bytes]:
    """Genera el cifrado de un archivo .txt UTF-8 en bloques de 1 MiB.

    Args:
        path: Archivo de texto regular de entrada.
        key: Clave de 1 a 64 letras ASCII.

    Yields:
        Bloques cifrados para guardar o mostrar; no crea un archivo de destino.

    Raises:
        ValidationError: Si la ruta no es un .txt regular o la clave no es válida.
        UnicodeDecodeError: Si el contenido no es UTF-8 válido.
        OSError: Si falla el acceso al origen.

    Al ser un generador, abre y valida la entrada cuando comienza la iteración.
    """
    yield from _transform_file(path, key, encrypt_chunks)


def decrypt_file(path: str | Path, key: str) -> Iterator[bytes]:
    """Genera el descifrado de un .txt con el contrato de encrypt_file.

    Lee bloques acotados y conserva BOM y saltos de línea. La clave y el UTF-8
    se validan al iterar; el consumidor es responsable de publicar el resultado.
    """
    yield from _transform_file(path, key, decrypt_chunks)


def _transform_file(path, key, transform):
    """Valida origen y clave y delega los bloques binarios al transformador."""
    source = input_text_file(path)
    key = validate_key(key)
    with source.open("rb") as stream:
        blocks = iter(lambda: stream.read(FILE_BLOCK_SIZE), b"")
        yield from transform(blocks, key)
