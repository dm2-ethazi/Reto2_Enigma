"""Cifrado AES-256-GCM por bloques, compatible con el contenedor R2AE v1."""

import os
import secrets
import string
import struct
from dataclasses import dataclass, field
from pathlib import Path

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

from ..errors import ValidationError
from ..utils.files import input_file, new_destination, staged_output

MAGIC = b"R2AE"
VERSION = 1
ITERATIONS = 200_000
SALT_SIZE = 16
NONCE_SIZE = 12
TAG_SIZE = 16
HEADER = struct.Struct(">4sBI16s12s")
HEADER_SIZE = HEADER.size
OVERHEAD = HEADER_SIZE + TAG_SIZE
CHUNK_SIZE = 1024 * 1024
PASSWORD_ALPHABET = string.ascii_uppercase + string.ascii_lowercase + string.digits
# Límite de un único mensaje GCM: 2**39 - 256 bits.
MAX_PLAINTEXT_SIZE = (1 << 36) - 32


@dataclass(frozen=True)
class EncryptionResult:
    path: Path
    password: str = field(repr=False)


def generate_password() -> str:
    return "".join(secrets.choice(PASSWORD_ALPHABET) for _ in range(16))


def _derive_key(password: str, salt: bytes) -> bytes:
    return PBKDF2HMAC(
        algorithm=hashes.SHA256(), length=32, salt=salt, iterations=ITERATIONS
    ).derive(password.encode("ascii"))


def encrypt_file(
    source: str | Path, destination: str | Path | None = None
) -> EncryptionResult:
    """Cifra cualquier archivo a partes de hasta 20 MiB y publica un único .enc."""
    source = input_file(source)
    destination = new_destination(
        destination if destination is not None else f"{source}.enc"
    )
    with source.open("rb") as original:
        if os.fstat(original.fileno()).st_size > MAX_PLAINTEXT_SIZE:
            raise ValidationError("El archivo supera el límite de un mensaje AES-GCM.")
        password = generate_password()
        salt = secrets.token_bytes(SALT_SIZE)
        nonce = secrets.token_bytes(NONCE_SIZE)
        header = HEADER.pack(MAGIC, VERSION, ITERATIONS, salt, nonce)
        encryptor = Cipher(
            algorithms.AES(_derive_key(password, salt)), modes.GCM(nonce)
        ).encryptor()
        encryptor.authenticate_additional_data(header)
        with staged_output(destination) as output:
            output.write(header)
            total = 0
            while block := original.read(CHUNK_SIZE):
                total += len(block)
                if total > MAX_PLAINTEXT_SIZE:
                    raise ValidationError(
                        "El archivo supera el límite de un mensaje AES-GCM."
                    )
                output.write(encryptor.update(block))
            output.write(encryptor.finalize())
            output.write(encryptor.tag)
    return EncryptionResult(destination, password)
