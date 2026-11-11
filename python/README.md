# Enigma — interfaz de terminal

Aplicación de terminal sin colores para cifrar y descifrar texto con Vigenère
y cualquier archivo con AES-256-GCM. El menú ofrece **Cifrar Vigenère**,
**Descifrar Vigenère**, **Cifrar archivo AES**, **Descifrar archivo AES** y **Salir**.
Las opciones están agrupadas bajo Vigenère / Texto y AES / Archivos.

Necesita Python 3.14 o posterior. AES utiliza `cryptography`, fijado en
`requirements.txt`; Vigenère y la interfaz usan la biblioteca estándar.

## Instalación y arranque

Desde esta carpeta, en Windows, crea el entorno e instala las dependencias una vez:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe main.py
```

También puedes abrir `iniciar_enigma.bat` con doble clic: utiliza automáticamente
`.venv` cuando existe. Si ejecutas `py -3 main.py` sin ese entorno, instala las
dependencias en ese mismo intérprete para usar AES.

En Debian o Ubuntu:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python main.py
```

Usa las flechas para navegar y Enter para elegir. También puedes pulsar
el número de una opción. Esc vuelve al menú o sale desde la pantalla principal.
Si la terminal no admite control de cursor, se muestran opciones numeradas:
escribe el número y pulsa Enter. `q` vuelve o sale. Ctrl+C también permite salir.

## Vigenère

1. Elige **Cifrar Vigenère**.
2. Introduce texto por consola o selecciona un archivo `.txt` UTF-8.
3. Introduce una clave de 1 a 64 letras A-Z, sin espacios, números ni acentos.
   La clave muestra un `*` por carácter en una terminal interactiva; Retroceso
   borra el último carácter y Esc cancela.
4. Elige **Mostrar**, **Guardar .txt** o **Mostrar y guardar**.
5. Si vas a guardar, selecciona el destino antes de iniciar el cifrado.
6. Espera al resultado y pulsa Enter para volver al menú.

Durante el cifrado o descifrado se muestra `Cifrando.` / `Descifrando.`,
con un ciclo de uno, dos y tres puntos que vuelve a empezar en una
misma línea, repitiendo los puntos sin colores. Al terminar se indica `Completado.`; si falla o se cancela,
se retira el indicador sin anunciar éxito. Si la salida se redirige a un archivo, solo se escribe
el mensaje `Cifrando...` una vez.

Ejemplo: `ATTACKATDAWN` con clave `LEMON` produce `LXFOPVEFRNHR`.
Solo las letras A-Z/a-z se transforman y avanzan la clave. Se conservan las
mayúsculas y minúsculas, los espacios, signos, números, acentos, ñ y otros
caracteres Unicode. Los archivos conservan sus saltos de línea CRLF/LF y el
BOM UTF-8, si existe. Vigenère es un cifrado educativo.

## AES

1. Elige **Cifrar archivo AES**.
2. Selecciona cualquier archivo de origen: texto, imagen, documento o binario.
3. Selecciona el destino; se propone `<nombre original>.enc`.
4. Durante `Cifrando AES...` se leen bloques de 1 MiB y se escriben directamente
   en `.part` de hasta 20 MiB en la carpeta de destino. Se mantienen una única
   operación AES-GCM y su etiqueta de autenticación entre todos los bloques.
5. Al completar el guardado se muestra una contraseña aleatoria de 16 caracteres
   A-Z, a-z y 0-9. Guárdala: no se escribe en el archivo ni se puede recuperar.
6. Pulsa Enter para volver al menú.

Cada operación genera una contraseña, una sal de 16 bytes y un nonce de 12 bytes
nuevos mediante `secrets`. Se deriva una clave de 256 bits con PBKDF2-HMAC-SHA256
y 200 000 iteraciones, como en el proyecto de referencia. El resultado usa el
contenedor R2AE v1: cabecera autenticada de 37 bytes, contenido cifrado y etiqueta
GCM de 16 bytes. Su tamaño es el original más 53 bytes; también admite archivos
vacíos. El límite de 20 MiB se aplica a todos los bytes de cada parte, incluida
la cabecera y la etiqueta. Consulta [el formato R2AE](docs/R2AE.md).

La contraseña solo se muestra después de guardar correctamente. Cancelar un
selector vuelve al menú sin cifrar; los errores de lectura o guardado se comunican
sin sobrescribir archivos existentes. El cifrado usa las primitivas de
[cryptography](https://cryptography.io/en/latest/hazmat/primitives/symmetric-encryption/).

## Selección y guardado de archivos

### Descifrado

Para Vigenère, elige **Descifrar Vigenère** e introduce el texto cifrado por consola
o selecciona un `.txt` UTF-8. Usa la misma clave del cifrado y elige mostrar,
guardar o ambas opciones. Por ejemplo, `LXFOPVEFRNHR` con `LEMON` recupera
`ATTACKATDAWN`. Se conservan BOM, Unicode y saltos de línea; la clave mantiene
su posición entre bloques. Vigenère no autentica los datos: una clave equivocada
produce otro texto y no puede detectarse automáticamente.

Para AES, elige **Descifrar archivo AES**, selecciona el contenedor R2AE e introduce
la contraseña original de 16 letras o números (con un `*` por carácter en terminal interactiva).
Elige un destino nuevo; se propone quitar la extensión `.enc`. El nombre original
no está almacenado en R2AE, por lo que puedes elegirlo manualmente.

El descifrado AES verifica el formato, versión y número de iteraciones antes de
derivar la clave. Usa los 37 bytes exactos de cabecera como AAD y verifica la
etiqueta GCM antes de publicar el resultado. Una contraseña incorrecta o un
archivo alterado/truncado devuelve un error y elimina las partes sin publicar
el archivo final. El archivo cifrado original se conserva.

Ambos descifrados trabajan en bloques de 1 MiB y usan `.part` de hasta 20 MiB,
igual que el cifrado, sin cargar archivos completos en memoria. En AES las partes
del descifrado contienen texto en claro pendiente de autenticación y se eliminan
al terminar, fallar o cancelar; un cierre abrupto puede dejar partes en disco.

Después de guardar correctamente un resultado de cifrado o descifrado, se abre
el explorador: Windows y macOS seleccionan el archivo; Linux abre su carpeta
con `xdg-open`. El archivo generado no se ejecuta. Al mostrar solo por consola
no se abre el explorador. Si no se puede iniciar, se mantiene el resultado y
se muestra su ruta.

### Selectores y publicación

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
      aes.py              # AES-256-GCM incremental con contenedor R2AE
    utils/
      __init__.py
      dialogs.py          # Selector nativo con alternativa por terminal
      files.py            # Lectura UTF-8 y guardado sin sobrescritura
    interface/
      __init__.py
      app.py              # Flujo entre menú y pantallas
      menu.py             # Opciones del menú principal
      vigenere.py         # Flujo de cifrado Vigenère
      aes.py              # Selección de origen/destino y cifrado AES
      layout.py           # Marcos y composición visual sin colores
      loading.py          # Indicador animado durante el cifrado
      navigation.py       # Selección de opciones y navegación
      terminal.py         # Lectura de teclas y compatibilidad Windows/Linux
  requirements.txt        # Dependencia cryptography para AES
  docs/R2AE.md            # Formato binario compartido con el proyecto original
  tests/                  # Algoritmos, archivos, selectores e interfaz
    fixtures/r2ae-v1.json  # Vectores públicos de prueba
```

`main.py` es un lanzador mínimo en la raíz de la aplicación. La implementación
se mantiene en `src/interface/`, con importaciones relativas entre sus módulos.

Los algoritmos residen en `src/encrypt/` y las utilidades de archivos en
`src/utils/`, para poder usarlos sin iniciar el menú.

## Pruebas

La integración continua ejecuta Ruff y pytest con una cobertura mínima del 80 %.
Desde la raíz del repositorio, instala uv y ejecuta la misma validación:

```powershell
python -m pip install uv
python scripts/quality.py ci
```

Las dependencias de CI están fijadas en `pyproject.toml` y `uv.lock`.
Consulta [la guía de CI](../docs/GUIA_CI_CD.md) para activar los hooks locales
y localizar los informes de pruebas y cobertura.

Desde esta carpeta, después de instalar `requirements.txt` en el entorno:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

En Linux, utiliza `.venv/bin/python`. Las pruebas de los selectores nativos
simulan Tkinter y no abren ventanas. Las pruebas AES comparan el cifrado con
vectores públicos y verifican el resultado con un consumidor independiente,
incluidos los límites de las partes, archivos binarios, cancelación y errores.
