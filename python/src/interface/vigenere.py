"""Pantallas de presentación de Vigenère."""

SCREENS = (
    (
        "Cifrar Vigenère",
        (
            ("Origen", "Texto por consola / archivo .txt"),
            ("Texto", "[Texto que se quiere cifrar]"),
            ("Clave", "[Clave de 1 a 64 letras]"),
            ("Resultado", "[Texto cifrado]"),
            ("Salida", "Mostrar / guardar .txt"),
        ),
    ),
    (
        "Descifrar Vigenère",
        (
            ("Origen", "Texto por consola / archivo .txt"),
            ("Texto", "[Texto que se quiere descifrar]"),
            ("Clave", "[Clave original]"),
            ("Resultado", "[Texto recuperado]"),
            ("Salida", "Mostrar / guardar .txt"),
        ),
    ),
)
