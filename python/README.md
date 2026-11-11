# Enigma · Python

Aplicación de terminal del **Reto 2 de DAM** para cifrar y descifrar texto y
archivos. Incluye Vigenère para texto UTF-8 y AES-256-GCM para archivos de
cualquier tipo, con verificación de autenticidad al descifrar.

[Instalación](#instalación) · [Uso](#uso) · [Vigenère](#vigenère) ·
[AES](#aes-256-gcm) · [Pruebas](#desarrollo-y-pruebas)

## Funcionalidades

| Función | Vigenère | AES-256-GCM |
| --- | --- | --- |
| Cifrar y descifrar | Sí | Sí |
| Entrada | Consola o archivos `.txt` UTF-8 | Cualquier archivo |
| Clave | Entre 1 y 64 letras A-Z | Contraseña aleatoria de 16 letras y números |
| Resultado | Mostrar, guardar `.txt` o ambas opciones | Guardar archivo |
| Autenticidad | Sin verificación | Verificación de etiqueta GCM y cabecera AAD |

Ambos algoritmos procesan archivos por bloques y utilizan temporales `.part`
de hasta **20 MiB**. Los destinos existentes se rechazan, se conservan los
originales y el explorador se abre después de guardar correctamente.

## Instalación

El entorno reproducible del proyecto utiliza **Python 3.14.x**
(`>=3.14,<3.15`) y las dependencias fijadas en `uv.lock`.
Los comandos siguientes parten de la raíz del repositorio.

### Con uv

```powershell
python -m pip install uv
cd python
python -m uv sync --locked
python -m uv run --locked python main.py
```

En Linux o macOS, utiliza `python3` si ese es el nombre de tu intérprete
Python 3.14. No necesitas activar manualmente el entorno creado por uv.

### Con venv y pip

Para ejecutar la aplicación sin instalar las herramientas de desarrollo:

**Windows (PowerShell)**

```powershell
cd python
py -3.14 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe main.py
```

**Linux / macOS**

```bash
cd python
python3.14 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python main.py
```

En Windows también puedes abrir `python/iniciar_enigma.bat` con doble clic
después de instalar las dependencias. El lanzador utiliza `python/.venv` cuando
existe; en caso contrario, intenta usar Python del sistema.

Los selectores gráficos requieren Tcl/Tk y una sesión de escritorio. Tkinter
es opcional y no se instala con pip: si no está disponible, la aplicación pide
las rutas por terminal.

## Uso

El menú agrupa las operaciones por algoritmo:

```text
VIGENÈRE / TEXTO
  1. Cifrar Vigenère
  2. Descifrar Vigenère

AES / ARCHIVOS
  3. Cifrar archivo AES
  4. Descifrar archivo AES

  5. Salir
```

- **Navegar:** flechas y Enter, o el número de una opción.
- **Volver o cancelar:** Esc. En el modo numerado, escribe `q` o `volver`.
- **Contraseñas:** en terminal interactiva aparece un `*` por carácter;
  Retroceso borra el último y Esc cancela. Los caracteres reales no se muestran.
- **Progreso:** los puntos repiten `. → .. → ... → .` mientras se cifra o
  descifra. Al terminar se muestra `Completado.`. Las operaciones rápidas
  pueden terminar antes de completar un ciclo.
- **Salida redirigida:** se utiliza un mensaje de progreso sencillo, sin animación.
- **Finalizar:** selecciona Salir o pulsa Ctrl+C.

Al guardar, Windows y macOS abren el explorador con el resultado seleccionado;
Linux abre su carpeta mediante `xdg-open`. El resultado no se ejecuta. Si no se
puede iniciar el explorador, se muestra la ruta y se conserva el archivo.

## Vigenère

1. Selecciona **Cifrar Vigenère** o **Descifrar Vigenère**.
2. Elige texto por consola o un archivo `.txt` en UTF-8.
3. Introduce la clave: de 1 a 64 letras A-Z, sin números, espacios ni acentos.
4. Elige **Mostrar**, **Guardar .txt** o **Mostrar y guardar**.
5. Si guardas, selecciona un destino nuevo y espera al resultado.
6. Pulsa Enter para volver al menú principal.

Ejemplo con la misma clave en ambas direcciones:

```text
Texto original:  ATTACKATDAWN
Clave:           LEMON
Texto cifrado:   LXFOPVEFRNHR
Texto descifrado: ATTACKATDAWN
```

Solo las letras A-Z/a-z se transforman y avanzan la posición de la clave.
Se conservan mayúsculas, minúsculas, números, espacios, signos, acentos, ñ,
Unicode, BOM UTF-8 y saltos de línea LF/CRLF. La posición de la clave se mantiene
entre bloques y partes. Un archivo con UTF-8 inválido se rechaza sin publicar
el resultado.

Vigenère es un cifrado educativo y no autentica los datos: una clave equivocada
produce otro texto y no puede detectarse automáticamente.

## AES-256-GCM

### Cifrar un archivo

1. Selecciona **Cifrar archivo AES** y elige el origen.
2. Elige un destino nuevo; se propone `<nombre original>.enc`.
3. Espera a que termine el cifrado y el guardado.
4. Guarda la contraseña generada de **16 caracteres A-Z, a-z y 0-9**.

La contraseña solo aparece después de guardar correctamente. No se almacena
en el contenedor ni se puede recuperar desde la aplicación.

### Descifrar un archivo

1. Selecciona **Descifrar archivo AES** y elige el archivo R2AE cifrado.
2. Introduce la contraseña original, respetando mayúsculas y minúsculas.
3. Elige un destino nuevo; para `.enc` se propone quitar esa extensión.
4. Espera al mensaje **Archivo descifrado y autenticado**.

El nombre original no se almacena en el contenedor. Si ya existe el destino
propuesto, elige otro nombre para conservar ambos archivos.

El descifrado comprueba formato, versión e iteraciones antes de derivar la
clave. Autentica los **37 bytes exactos de cabecera como AAD** y verifica la
etiqueta GCM de 16 bytes antes de publicar el resultado. Una contraseña
incorrecta, una cabecera alterada o datos dañados producen un error sin publicar
el archivo final.

El contenedor **R2AE v1** utiliza PBKDF2-HMAC-SHA256 con 200 000 iteraciones,
sal de 16 bytes y nonce de 12 bytes nuevos por cifrado. Añade 53 bytes al tamaño
original y admite archivos vacíos. Consulta [la especificación R2AE](docs/R2AE.md)
para ver el formato y los vectores de prueba.

## Archivos y partes temporales

- Las lecturas se hacen en bloques de **1 MiB**, sin cargar archivos completos
  en memoria. El texto introducido por consola se procesa en memoria.
- Cada `.part` ocupa como máximo **20 MiB (20 971 520 bytes)**. Las partes se
  unen en orden para generar un único resultado; no son archivos cifrados
  independientes.
- Las partes se crean en la carpeta de destino. Para Vigenère con la opción
  **Mostrar**, se utiliza la carpeta temporal del sistema.
- Los temporales se eliminan al completar, cancelar o producirse un error
  controlado. En el descifrado AES contienen datos en claro pendientes de
  autenticación; el destino final se publica después de verificar la etiqueta.
- Una única parte se publica mediante enlace duro, que requiere un sistema de
  archivos compatible, como NTFS o ext4. Varias partes se unen en un archivo
  nuevo visible mientras se completa; un error controlado elimina ese destino
  incompleto. Un cierre abrupto puede dejar partes o un resultado incompleto.

## Desarrollo y pruebas

Desde la **raíz del repositorio**, ejecuta las mismas revisiones que CI:

```powershell
python -m pip install uv
python -m unittest discover -s scripts -p 'test_*.py' -v
python scripts/quality.py ci
```

La validación instala las dependencias con `uv sync --locked`, comprueba formato
y lint con Ruff, y ejecuta pytest con cobertura de ramas mínima del **80 %**.
Los informes JUnit y cobertura XML/HTML se guardan en `.artifacts/python/`.

Para corregir el formato desde la raíz:

```powershell
python -m uv run --locked --project python ruff format python
```

Para ejecutar únicamente las pruebas de aplicación desde `python/`, después de
crear el entorno e instalar `requirements.txt`:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

En Linux/macOS, sustituye el intérprete por `.venv/bin/python`. Las pruebas
simulan los selectores y el explorador para no abrir ventanas. Cubren vectores
AES de referencia, verificación AAD, contraseñas incorrectas, Vigenère, límites
de partes, cancelación, errores y entrada de claves enmascaradas.

Consulta [la guía de CI](../docs/GUIA_CI_CD.md) para activar los hooks locales,
ver qué ramas se validan y localizar los artefactos en GitHub Actions.

## Estructura

```text
python/
├── main.py                  # Punto de entrada
├── iniciar_enigma.bat        # Lanzador Windows
├── pyproject.toml            # Dependencias y configuración de calidad
├── uv.lock                   # Entorno reproducible de aplicación y desarrollo
├── requirements.txt          # Dependencia de ejecución para instalación con pip
├── docs/R2AE.md              # Especificación del contenedor AES
├── src/
│   ├── errors.py             # Validación y cancelación controladas
│   ├── encrypt/              # Algoritmos AES y Vigenère por bloques
│   ├── interface/            # Menú, pantallas, progreso y claves enmascaradas
│   └── utils/                # Selectores, partes, publicación y explorador
└── tests/
    └── fixtures/r2ae-v1.json # Vectores públicos de referencia
```

## Problemas frecuentes

| Problema | Qué hacer |
| --- | --- |
| Falta `cryptography` | Instala `requirements.txt` con el mismo intérprete que ejecuta `main.py`, o utiliza uv. |
| No se abre el selector gráfico | Introduce la ruta por terminal; admite espacios, comillas y `~`. Una entrada vacía cancela. |
| El destino ya existe | Elige un nombre nuevo; no se sobrescriben archivos. |
| Vigenère rechaza un archivo | Usa un `.txt` válido en UTF-8 y una clave de 1 a 64 letras A-Z. |
| AES no autentica el archivo | Comprueba la contraseña y que el archivo esté completo y sin modificar. |
| No se abre el explorador | Usa la ruta mostrada para abrir la carpeta manualmente. |
