# Enigma — interfaz de terminal

Versión de presentación: menú y cuatro pantallas de Vigenère y AES, sin colores.
Los campos son ejemplos visuales. Esta versión no solicita claves, no abre
archivos, no cifra ni descifra y no guarda resultados. Solo permite navegar.

Necesita Python 3.14 o posterior y no tiene dependencias externas.

Desde esta carpeta, en Windows:

```powershell
py -3 main.py
```

También puedes abrir `iniciar_enigma.bat` con doble clic.

En Debian o Ubuntu:

```bash
python3 main.py
```

Usa las flechas para navegar y Enter para elegir. También puedes pulsar
el número de una opción. Esc vuelve al menú o sale desde la pantalla principal.
Si la terminal no admite control de cursor, se muestran opciones numeradas:
escribe el número y pulsa Enter. `q` vuelve o sale. Ctrl+C también permite salir.

## Estructura

```text
python/
  main.py                 # Punto de entrada
  iniciar_enigma.bat       # Lanzador Windows
  src/
    __init__.py
    interface/
      __init__.py
      app.py              # Flujo entre menú y pantallas
      menu.py             # Opciones del menú principal
      vigenere.py         # Pantallas de cifrado/descifrado Vigenère
      aes.py              # Pantallas de cifrado/descifrado AES
      layout.py           # Marcos y composición visual sin colores
      navigation.py       # Selección de opciones y navegación
      terminal.py         # Lectura de teclas y compatibilidad Windows/Linux
```

`main.py` es un lanzador mínimo en la raíz de la aplicación. La implementación
se mantiene en `src/interface/`, con importaciones relativas entre sus módulos.
