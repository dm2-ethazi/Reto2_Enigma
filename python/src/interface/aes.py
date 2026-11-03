"""Pantallas de presentación de archivos AES."""

SCREENS = (
    (
        "Cifrar archivo AES",
        (
            ("Archivo", "[Seleccionar archivo de origen]"),
            ("Destino", "[Seleccionar destino .enc]"),
            ("Contraseña", "[Contraseña generada]"),
            ("Estado", "[Estado del cifrado]"),
        ),
    ),
    (
        "Descifrar archivo AES",
        (
            ("Archivo", "[Seleccionar archivo .enc]"),
            ("Destino", "[Seleccionar destino recuperado]"),
            ("Contraseña", "[Contraseña original]"),
            ("Estado", "[Estado del descifrado]"),
        ),
    ),
)
