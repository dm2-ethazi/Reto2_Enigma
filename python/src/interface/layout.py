"""Composición de las pantallas y los marcos, sin colores."""

import shutil
from textwrap import wrap


def render(title, choices, selected, fields, interactive):
    columns = shutil.get_terminal_size().columns
    width = min(66, max(24, columns - 4))
    lines = [
        "ENIGMA / RETO 02",
        "Cifrado de texto y archivos",
        "",
        title,
        "-" * min(40, width),
    ]
    for label, placeholder in fields:
        lines.extend([label, "  " + placeholder, ""])
    main_menu = title == "Menú principal"
    if main_menu:
        lines.extend(["VIGENÈRE / TEXTO", ""])
    for index, label in enumerate(choices):
        if main_menu and label == "Cifrar archivo AES":
            lines.extend(["", "AES / ARCHIVOS", ""])
        if main_menu and label == "Salir":
            lines.append("")
        marker = ">" if interactive and index == selected else " "
        lines.append(f"{marker} {index + 1}. {label}")
    lines.extend(
        [
            "",
            "Flechas o j/k: mover | Enter: elegir | Esc: volver"
            if interactive
            else "Escribe el número de una opción y pulsa Enter.",
        ]
    )
    lines = [part for line in lines for part in (wrap(line, width=width) or [""])]
    if columns < 44 or len(lines) + 2 > shutil.get_terminal_size().lines:
        return "\n".join(lines)
    border = "+" + "-" * (width + 2) + "+"
    return "\n".join([border, *(f"| {line:<{width}} |" for line in lines), border])
