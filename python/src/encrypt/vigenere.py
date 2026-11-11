"""Vigenère educativo: solo A-Z avanza la clave; se conserva el resto del texto."""

import codecs
from collections.abc import Iterable, Iterator
from pathlib import Path

from ..errors import ValidationError
from ..utils.files import input_text_file

FILE_BLOCK_SIZE = 1024 * 1024


def validate_key(key: str) -> str:
    if not 1 <= len(key) <= 64 or not all(
        "A" <= c <= "Z" or "a" <= c <= "z" for c in key
    ):
        raise ValidationError(
            "La clave Vigenère debe contener entre 1 y 64 letras A-Z."
        )
    return key.upper()


def encrypt(text: str, key: str) -> str:
    return _transform(text, key, 1)


def decrypt(text: str, key: str) -> str:
    return _transform(text, key, -1)


def _transform(text: str, key: str, direction: int) -> str:
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
            continue
        shift = ord(key[index % len(key)]) - ord("A")
        result.append(chr(base + (ord(character) - base + direction * shift) % 26))
        index += 1
    return "".join(result)


def encrypt_chunks(chunks: Iterable[bytes], key: str) -> Iterator[bytes]:
    """Cifra bloques UTF-8 manteniendo la posición de la clave entre bloques."""
    yield from _transform_chunks(chunks, key, 1)


def decrypt_chunks(chunks: Iterable[bytes], key: str) -> Iterator[bytes]:
    """Descifra UTF-8 conservando la posición de clave, BOM y saltos de línea."""
    yield from _transform_chunks(chunks, key, -1)


def _transform_chunks(chunks, key, direction):
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
    decoder.decode(b"", final=True)


def encrypt_file(path: str | Path, key: str) -> Iterator[bytes]:
    """Lee y cifra un .txt en bloques de 1 MiB sin cargar el archivo completo."""
    yield from _transform_file(path, key, encrypt_chunks)


def decrypt_file(path: str | Path, key: str) -> Iterator[bytes]:
    """Lee y descifra un .txt por bloques para escribir o mostrar desde partes."""
    yield from _transform_file(path, key, decrypt_chunks)


def _transform_file(path, key, transform):
    source = input_text_file(path)
    key = validate_key(key)
    with source.open("rb") as stream:
        blocks = iter(lambda: stream.read(FILE_BLOCK_SIZE), b"")
        yield from transform(blocks, key)
