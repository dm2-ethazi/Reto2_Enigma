# Contenedor R2AE v1

Formato del proyecto de referencia, usado por `src/encrypt/aes.py` para cifrar.

| Offset | Bytes | Campo |
| --- | --- | --- |
| 0 | 4 | ASCII `R2AE` |
| 4 | 1 | Versión `01` |
| 5 | 4 | Iteraciones PBKDF2: 200 000, uint32 big-endian |
| 9 | 16 | Sal aleatoria |
| 25 | 12 | Nonce GCM aleatorio |
| 37 | N | Contenido cifrado |
| 37 + N | 16 | Etiqueta GCM |

El tamaño total es N + 53 bytes. Un archivo vacío produce 53 bytes. La contraseña,
la clave y el nombre original no se incluyen en el contenedor.

Se genera una contraseña uniforme de 16 caracteres ASCII del conjunto
`ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789`, una sal de
16 bytes y un nonce de 12 bytes nuevos por operación, usando `secrets`.

La clave de 32 bytes se deriva mediante PBKDF2-HMAC-SHA256, con 200 000 iteraciones
y la contraseña codificada en ASCII. Se cifra con AES-256-GCM y se autentican
los 37 bytes exactos de cabecera como AAD. El cifrador se mantiene entre lecturas
de 1 MiB y entre partes temporales; al finalizar se añade su etiqueta de 128 bits.
El formato final es `cabecera || ciphertext || tag`.

Los `.part` son fragmentos temporales de hasta 20 MiB del mismo contenedor.
No contienen texto en claro ni se cifran como mensajes separados. Se unen en
orden para producir un único archivo y se eliminan al salir de la operación.
El máximo admitido por mensaje GCM es `2**36 - 32` bytes de entrada; la aplicación
lo comprueba al abrir el archivo y durante la lectura.

`tests/fixtures/r2ae-v1.json` contiene vectores públicos de prueba del proyecto
original: archivo vacío, un byte, texto UTF-8, datos binarios y un PNG. Sus
contraseñas, sales y nonces son exclusivamente datos de prueba.

Primitivas proporcionadas por `cryptography`:

- [AES-GCM incremental y autenticación de cabeceras](https://cryptography.io/en/latest/hazmat/primitives/symmetric-encryption/).
- [PBKDF2-HMAC](https://cryptography.io/en/latest/hazmat/primitives/key-derivation-functions/#pbkdf2).
