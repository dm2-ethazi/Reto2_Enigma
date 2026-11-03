# Enigma — interfaz de terminal

Esta fase incluye únicamente cifrado Vigenère para texto por consola o archivos
`.txt` UTF-8, con una interfaz de terminal sin colores. El menú ofrece
**Cifrar Vigenère** y **Salir**.

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

## Vigenère

1. Elige **Cifrar Vigenère**.
2. Introduce texto por consola o selecciona un archivo `.txt` UTF-8.
3. Introduce una clave de 1 a 64 letras A-Z, sin espacios, números ni acentos.
   La clave se oculta al escribirla en una terminal interactiva.
4. Elige **Mostrar**, **Guardar .txt** o **Mostrar y guardar**.
5. Si vas a guardar, selecciona el destino antes de iniciar el cifrado.
6. Espera al resultado y pulsa Enter para volver al menú.

Durante el cifrado se muestra `Cifrando.`, `Cifrando..`, `Cifrando...` en una
misma línea, repitiendo los puntos sin colores. El indicador desaparece al
terminar o cancelar. Si la salida se redirige a un archivo, solo se escribe
el mensaje `Cifrando...` una vez.

Ejemplo: `ATTACKATDAWN` con clave `LEMON` produce `LXFOPVEFRNHR`.
Solo las letras A-Z/a-z se transforman y avanzan la clave. Se conservan las
mayúsculas y minúsculas, los espacios, signos, números, acentos, ñ y otros
caracteres Unicode. Los archivos conservan sus saltos de línea CRLF/LF y el
BOM UTF-8, si existe. Vigenère es un cifrado educativo.

## Selección y guardado de archivos

La función reutilizable `choose_path` de `src/utils/dialogs.py` abre un selector
nativo mediante Tkinter. Admite filtros, nombre sugerido y extensión para guardar.
Si Tkinter no está disponible o no hay escritorio, solicita la ruta en la terminal;
admite rutas entre comillas, espacios y `~`. Una entrada vacía cancela.

Cancelar el selector o pulsar Esc en un submenú vuelve al menú principal.
Los errores de clave, lectura o guardado se muestran sin cerrar la aplicación.
Los archivos originales se mantienen intactos y nunca se sobrescriben destinos
existentes. El resultado se prepara en archivos temporales `.part` de hasta
20 MiB (20 971 520 bytes) cada uno. Si supera ese tamaño, se crean tantas partes
como sean necesarias y se unen en orden para obtener un único archivo de salida.
El límite se aplica a bytes, también cuando el texto contiene caracteres UTF-8
que ocupan varios bytes. Las partes se eliminan al completar o cancelar la
operación, y también ante errores controlados. Al cifrar un archivo, las partes
se escriben progresivamente mientras aparece `Cifrando...`, en la carpeta elegida
para el resultado. Si eliges solo **Mostrar**, se crean en la carpeta temporal
del sistema y también se eliminan después de mostrar el texto.

Cuando basta una parte, se publica mediante un enlace duro atómico; el sistema
de archivos de destino debe admitirlos, como NTFS o ext4. Cuando hay varias, se
unen directamente en un archivo de destino nuevo, sin crear un temporal adicional
mayor de 20 MiB. Ese archivo es visible mientras se completa: un error controlado
lo elimina, pero un cierre abrupto del proceso puede dejarlo incompleto y conservar
partes temporales. No se reemplaza ningún archivo existente, aunque otro proceso
lo cree durante la operación.

Los archivos de entrada se leen en bloques de 1 MiB y se cifran directamente a
las partes, sin cargar el archivo ni el resultado completo en memoria. La clave
continúa desde su posición anterior entre bloques y partes. La validación UTF-8
también es incremental: admite caracteres divididos entre lecturas y rechaza
secuencias inválidas o incompletas sin guardar un resultado. Para **Mostrar**, el
resultado se lee de las partes por bloques. El texto escrito por consola sigue
cifrándose en memoria.

Tkinter es opcional y no se instala con pip. Para usar los selectores nativos,
la instalación de Python debe incluir Tcl/Tk y disponer de una sesión gráfica.

## Estructura

```text
python/
  main.py                 # Punto de entrada
  iniciar_enigma.bat       # Lanzador Windows
  src/
    __init__.py
    errors.py             # Validación y cancelación controladas
    encrypt/
      __init__.py
      vigenere.py         # Algoritmo de cifrado independiente de la interfaz
    utils/
      __init__.py
      dialogs.py          # Selector nativo con alternativa por terminal
      files.py            # Lectura UTF-8 y guardado sin sobrescritura
    interface/
      __init__.py
      app.py              # Flujo entre menú y pantallas
      menu.py             # Opciones del menú principal
      vigenere.py         # Flujo de cifrado Vigenère
      layout.py           # Marcos y composición visual sin colores
      loading.py          # Indicador animado durante el cifrado
      navigation.py       # Selección de opciones y navegación
      terminal.py         # Lectura de teclas y compatibilidad Windows/Linux
  tests/                  # Algoritmo, archivos, selectores e interfaz
```

`main.py` es un lanzador mínimo en la raíz de la aplicación. La implementación
se mantiene en `src/interface/`, con importaciones relativas entre sus módulos.

El algoritmo reside en `src/encrypt/vigenere.py` y las utilidades de archivos en
`src/utils/`, para poder usarlos sin iniciar el menú.

## Pruebas

Desde esta carpeta, sin dependencias adicionales:

```powershell
py -3 -m unittest discover -s tests -v
```

En Linux, utiliza `python3` en lugar de `py -3`. Las pruebas de los selectores
nativos simulan Tkinter y no abren ventanas.
