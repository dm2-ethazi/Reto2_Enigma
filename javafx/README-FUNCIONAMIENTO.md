# Gestor Enigma AES: funcionamiento de la aplicación

Este documento explica el flujo actual de la aplicación JavaFX: selección de
archivos, cifrado, descifrado, verificación y tratamiento de errores. No
modifica ni reemplaza el [README.md](./README.md) existente.

> **Nota sobre el contrato del reto:** el contrato técnico no está incluido en
> el repositorio (la carpeta `docs` solo contiene un archivo marcador). Por
> ello, esta guía describe el comportamiento que se puede comprobar en el
> código y las pruebas actuales; no certifica por sí sola el cumplimiento de
> requisitos que no están disponibles aquí.

## Resumen

La aplicación cifra y descifra archivos en un único flujo AES-256-GCM. Lee y
escribe el contenido en bloques de 64 KiB y reutiliza sus buffers. Los bloques
no son archivos independientes ni se pueden recuperar por separado.

Durante el cifrado, el código de la aplicación procesa el archivo en bloques
sin cargarlo entero en memoria. En descifrado, algunos proveedores de AES-GCM
(incluido el proveedor estándar de Java) retienen el texto claro hasta
comprobar la etiqueta y pueden devolverlo completo al final. Por tanto, aunque
la aplicación alimenta el cifrador por bloques y escribe a un temporal, el uso
de memoria del descifrado puede crecer con el tamaño del archivo.

El resultado se escribe primero en un archivo temporal con sufijo `.part`.
Solo se mueve al nombre definitivo después de completar la operación. Al
descifrar, esto ocurre después de que GCM haya verificado la etiqueta de
autenticación; si se indicó un archivo de referencia, también después de
comprobar que ambos archivos contienen exactamente los mismos bytes.

## Navegación con teclado

Los siguientes atajos están disponibles en la ventana principal y también se
muestran en **Ayuda**:

- `F1`: abrir la ayuda.
- `F2`: mostrar información acerca de la aplicación.
- `Ctrl+1`: abrir la vista de cifrado.
- `Ctrl+2`: abrir la vista de descifrado.
- `Ctrl+Enter`: iniciar la operación de la vista actual.
- `Esc`: volver al menú principal.

## Recorrido de cifrado

1. En la pantalla principal se selecciona un archivo `.txt`, `.png` u otro fichero binario y una carpeta de destino. El selector recuerda la última carpeta usada para cada tipo de selección, incluso al cerrar y volver a abrir la aplicación.
2. `MainController` valida los campos básicos e inicia el trabajo en una tarea
   de JavaFX ejecutada en segundo plano.
3. `FileService` comprueba que el origen sea un archivo normal y legible, y
   que el destino sea un directorio escribible. Si ya existe el `.enc`, no lo
   sobrescribe.
4. Se crea un temporal `.r2ae-....part` en la carpeta de destino.
5. `CryptoService` genera sal y nonce aleatorios, deriva una clave con
   PBKDF2-HMAC-SHA256 (200 000 iteraciones) y cifra el flujo con
   AES-256-GCM. La cabecera R2AE también se autentica como dato asociado.
6. Al terminar el flujo criptográfico, el temporal se mueve al nombre
   `<archivo-original>.enc`. La contraseña generada se muestra al usuario y
   debe guardarse de forma segura, separada del contenedor.
7. Si algo falla antes de publicar el resultado, `FileService` intenta borrar
   el temporal.

Los tres campos de ruta de archivo aceptan arrastrar y soltar un único fichero:
el origen y la referencia admiten cualquier fichero normal legible; el
contenedor para descifrar debe terminar en `.enc`. Los selectores aplican los
mismos criterios.

## Recorrido de descifrado y verificación

1. Se seleccionan el contenedor `.enc`, una carpeta de destino y la contraseña.
2. Opcionalmente, se activa la verificación y se selecciona el archivo original
   de referencia.
3. `FileService` comprueba las rutas, la extensión y el tamaño mínimo del
   contenedor, y rechaza destinos que ya existan.
4. El contenido descifrado se escribe en un temporal `.part`, no directamente
   en el archivo final.
5. AES-GCM verifica la etiqueta al finalizar. Una contraseña incorrecta o una
   alteración de la cabecera autenticada o del contenido provoca un error.
6. Si se indicó una referencia, `Files.mismatch` compara el temporal y la
   referencia byte a byte. Una diferencia impide publicar el resultado.
7. Solo si todas las comprobaciones terminan correctamente se mueve el
   temporal al nombre definitivo. En caso de error se elimina.

La autenticación GCM comprueba la integridad del contenedor y que se utilizó la
clave correcta; no identifica por sí misma a la persona que lo creó. La
comparación opcional con una referencia añade una comprobación de igualdad con
ese archivo, pero tampoco constituye una firma de identidad.

## Vista previa después de descifrar

La vista previa no se genera al seleccionar un contenedor ni al escribir la
contraseña. Para mostrar su contenido, selecciona el contenedor `.enc`, indica
la contraseña y pulsa **Descifrar archivo**. Primero se completa correctamente
el descifrado y se guarda el archivo; después, si es un `.txt.enc` o `.png.enc`,
se prepara la vista previa. Si se cambia la ruta o la contraseña, el contenido
anterior se borra para evitar mostrar datos que ya no corresponden a la
selección actual.

La vista previa se descifra y autentica en segundo plano. Solo después de que
AES-GCM valide el contenedor se enseña el resultado; si la contraseña no es
correcta o el archivo está alterado, el panel queda vacío y muestra el error.
No se crea un archivo de salida ni un temporal `.part` para esta operación.

- Para `.txt.enc` se muestran como máximo los primeros 256 KiB, interpretados
  como UTF-8. Si el archivo es mayor, la etiqueta indica que el texto está
  truncado y muestra los tamaños.
- Para `.png.enc` se conserva como máximo 8 MiB y se comprueba la firma y las
  dimensiones antes de cargar la imagen. Si excede el límite o no es un PNG
  válido, se informa sin mostrar una imagen parcial.
- Otros formatos se pueden descifrar normalmente, pero no tienen vista previa
  en esta versión.

El límite de memoria controla los bytes que el servicio de archivos conserva
para la interfaz. Como se explica en la sección de chunks, el proveedor
criptográfico puede mantener adicionalmente texto claro hasta comprobar la
etiqueta GCM; la vista previa no cambia esa característica del descifrado AES.

## Qué son los chunks y cómo se procesan

Un chunk (bloque) es una porción del flujo que se procesa antes de leer la
siguiente. En `CryptoService` el tamaño de entrada es `64 * 1024` bytes (64
KiB). Por ejemplo, un archivo de 1 MiB se procesa en 16 bloques de entrada,
además del último bloque si el tamaño no es múltiplo de 64 KiB.

En cada iteración:

1. Se lee un bloque en el arreglo de entrada reutilizable.
2. `Cipher.update` cifra o descifra ese bloque en un arreglo de salida
   reutilizable. Se crea una vez por operación con el tamaño máximo calculado
   mediante `Cipher.getOutputSize`, y se reutiliza en las iteraciones.
3. Se escriben en el flujo de salida únicamente los bytes que el cifrador
   informa que produjo.
4. Se actualiza el contador de progreso.

Al final, `Cipher.doFinal` completa el flujo. En cifrado emite los bytes
pendientes y la etiqueta GCM; en descifrado comprueba la etiqueta. La salida
final del descifrado se recibe en un arreglo aparte porque el proveedor puede
haber retenido todo el texto claro hasta validar GCM.

### Optimización aplicada

Antes, cada llamada a `Cipher.update` devolvía un arreglo nuevo. Ahora, cada
operación crea un arreglo de salida del tamaño máximo necesario y lo reutiliza
en todos los bloques. Esto reduce las asignaciones de memoria por bloque y la
presión sobre el recolector de basura, sin cambiar el tamaño de los bloques ni
el formato R2AE v1. La salida final del descifrado puede seguir siendo
proporcional al tamaño del archivo, según el proveedor criptográfico de Java.

La optimización no convierte el archivo en varias piezas persistentes: sigue
habiendo un solo `.enc` o archivo recuperado y un solo temporal durante la
operación. Tampoco ofrece reanudación, acceso aleatorio ni descifrado aislado
por bloque.

## Papel de las clases principales

| Clase | Responsabilidad |
|---|---|
| `Launcher` | Punto de entrada Java que inicia JavaFX. |
| `MainApplication` | Carga FXML y CSS, crea y muestra la ventana. |
| `MainController` | Gestiona los controles, valida campos básicos, muestra el progreso y presenta errores. |
| `FileService` | Valida rutas, crea temporales, coordina el cifrado/descifrado y publica o elimina resultados. |
| `CryptoService` | Genera contraseñas y procesa flujos R2AE v1 con AES-256-GCM. |
| `LanguageService` | Traduce textos de la interfaz; no modifica el contenido de los archivos. |

## Errores que se muestran

La interfaz presenta un aviso si, entre otros casos:

- faltan rutas o la contraseña;
- una ruta no es válida, el archivo no existe o no se puede leer;
- el destino no es un directorio escribible;
- el contenedor no tiene el formato esperado o está incompleto;
- la contraseña es incorrecta o el archivo ha sido alterado;
- ya existe un archivo con el nombre de salida;
- el archivo recuperado no coincide con la referencia.

Un fallo de autenticación puede ocurrir después de que GCM haya producido
bytes descifrados al temporal. Por esa razón el temporal nunca se trata como
resultado válido antes de que `doFinal` verifique la etiqueta.

## Pruebas relacionadas

Las pruebas JavaFX se ejecutan desde la carpeta `javafx` con:

```powershell
mvn test
```

Incluyen pruebas de límites de chunk (justo antes, en y después de 64 KiB),
progreso, autenticidad, contraseña incorrecta, referencia distinta, limpieza
de temporales y round-trips de archivos de varios tamaños.

## Crear el JAR ejecutable

Se necesita JDK 25 y Maven 3.6.3 o superior. El POM comprueba la versión de
Maven al iniciar la compilación y muestra un error claro si no se cumple el
mínimo. Desde esta carpeta, ejecuta:

```powershell
mvn clean package
```

El POM selecciona automáticamente los componentes nativos de JavaFX para el
sistema donde se ejecuta Maven y genera un JAR con ese destino:

- Windows: `target/Enigma-GestorAES-Java25-windows.jar`
- Ubuntu o Debian: `target/Enigma-GestorAES-Java25-linux.jar`

Hay que construir el paquete en cada sistema operativo de destino; un JAR
con librerías nativas de Windows no sustituye al de Linux, ni viceversa. Para
obtener ambos, ejecuta el mismo comando una vez en Windows y otra en Ubuntu o
Debian. El JAR incluye JavaFX, pero no el propio JDK: para ejecutarlo hace falta
tener Java 25 instalado en el sistema.
