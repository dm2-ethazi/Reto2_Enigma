"""Composición de las pantallas y los marcos, sin colores."""

import shutil
from textwrap import wrap

from ..i18n import tr


def render(title, choices, selected, fields, interactive):
    """Compone una pantalla de texto adaptada al tamaño de la terminal.

    Args:
        title: Título de la pantalla; admite las claves de traducción de la app.
        choices: Etiquetas de opciones en el orden de selección.
        selected: Índice desde cero de la opción marcada.
        fields: Pares de etiqueta y texto de ayuda para campos informativos.
        interactive: Muestra el marcador y la ayuda de navegación por teclas.

    Returns:
        Texto sin colores; añade un marco si hay espacio y divide líneas largas.
        El menú principal incluye encabezados de Vigenère y AES.
    """
    main_menu = title in ("Menú principal", tr("Menú principal"))
    columns = shutil.get_terminal_size().columns
    width = min(66, max(24, columns - 4))
    lines = [
        "ENIGMA / RETO 02",
        tr("Cifrado de texto y archivos"),
        "",
        tr(title),
        "-" * min(40, width),
    ]
    for label, placeholder in fields:
        lines.extend([label, "  " + placeholder, ""])
    if main_menu:
        lines.extend([tr("VIGENÈRE / TEXTO"), ""])
    for index, label in enumerate(choices):
        label = tr(label)
        if main_menu and label == tr("Cifrar archivo AES"):
            lines.extend(["", tr("AES / ARCHIVOS"), ""])
        if main_menu and label == tr("Salir"):
            lines.append("")
        marker = ">" if interactive and index == selected else " "
        lines.append(f"{marker} {index + 1}. {label}")
    lines.extend(
        [
            "",
            tr("Flechas o j/k: mover | Enter: elegir | Esc: volver")
            if interactive
            else tr("Escribe el número de una opción y pulsa Enter."),
        ]
    )
    lines = [part for line in lines for part in (wrap(line, width=width) or [""])]
    if columns < 44 or len(lines) + 2 > shutil.get_terminal_size().lines:
        return "\n".join(lines)
    border = "+" + "-" * (width + 2) + "+"
    return "\n".join([border, *(f"| {line:<{width}} |" for line in lines), border])
