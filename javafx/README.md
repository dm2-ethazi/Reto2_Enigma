# Gestor AES - guía técnica de JavaFX

Este documento describe la aplicación de escritorio JavaFX que se encuentra en
esta carpeta: cómo arranca, cómo se relacionan sus clases y recursos, qué
funciones están operativas y cuáles siguen pendientes. Se centra en el código
Java y explica con fragmentos seleccionados las partes que requieren más
contexto. No sustituye a los documentos generales del Reto 2 ni al contrato
técnico.

> **Estado funcional:** la selección de ficheros y carpetas, el cifrado AES
> (R2AE v1), la generación de contraseña, el progreso textual y la copia de la
> contraseña están implementados. El descifrado, la comparación con un original
> y la vista previa aparecen en la interfaz, pero todavía no están conectados a
> una implementación. «Ayuda» y «Acerca de» también muestran un aviso de función
> pendiente.

## Índice

1. [Qué hace la aplicación](#qué-hace-la-aplicación)
2. [Estructura del proyecto](#estructura-del-proyecto)
3. [Recorrido de una operación de cifrado](#recorrido-de-una-operación-de-cifrado)
4. [Clases de producción](#clases-de-producción)
   - [Launcher](#launcher)
   - [MainApplication](#mainapplication)
   - [MainController](#maincontroller)
   - [CryptoService](#cryptoservice)
   - [FileService](#fileservice)
5. [Interfaz FXML y estilos](#interfaz-fxml-y-estilos)
6. [Clases de prueba](#clases-de-prueba)
7. [Formato R2AE v1](#formato-r2ae-v1)
8. [Compilar, ejecutar y probar](#compilar-ejecutar-y-probar)

## Qué hace la aplicación

Gestor AES es una aplicación de escritorio para proteger ficheros locales.
Permite seleccionar o arrastrar un fichero de entrada, elegir una carpeta de
salida y cifrar el contenido. El archivo original se conserva y el resultado se
guarda con la extensión `.enc`. La contraseña que se genera para ese cifrado se
muestra inicialmente oculta y se puede revelar o copiar.

El pliego del reto solicita que el cliente de escritorio pueda seleccionar un
fichero de entrada y, cuando corresponda, una ubicación de salida. Ese flujo de
selección está disponible en esta versión. La interfaz incluye además controles
pensados para la futura recuperación de ficheros, pero no se debe confundir su
presencia visual con una función terminada.

### Funciones disponibles y pendientes

| Funcionalidad | Estado en el código actual |
|---|---|
| Seleccionar fichero de entrada con el diálogo del sistema | Implementada |
| Soltar un único fichero sobre el campo de origen | Implementada |
| Seleccionar carpeta de destino | Implementada |
| Seleccionar un archivo original de referencia | El selector está implementado; la verificación aún no |
| Limpiar rutas y contraseñas de la pantalla | Implementada; no borra archivos |
| Cifrar un fichero con AES-256-GCM | Implementada |
| Generar y mostrar una contraseña segura de 16 caracteres | Implementada |
| Mostrar u ocultar la contraseña generada | Implementada |
| Copiar la contraseña al portapapeles | Implementada |
| Descifrar contenedores `.enc` | Pendiente |
| Comparar el resultado con un original | Pendiente |
| Vista previa de texto o imagen recuperados | Pendiente |
| Ayuda y ventana «Acerca de» | Pendientes; presentan un aviso informativo |

## Estructura del proyecto

```text
javafx/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/enigma/javafx/
    │   │   ├── Launcher.java
    │   │   ├── MainApplication.java
    │   │   ├── controller/MainController.java
    │   │   └── service/
    │   │       ├── CryptoService.java
    │   │       └── FileService.java
    │   └── resources/com/enigma/javafx/
    │       ├── main-view.fxml
    │       └── styles.css
    └── test/java/com/enigma/javafx/
        ├── controller/MainControllerTest.java
        └── service/
            ├── CryptoServiceTest.java
            └── FileServiceTest.java
```

La responsabilidad está separada en capas:

1. `Launcher` inicia JavaFX.
2. `MainApplication` carga la ventana y sus recursos.
3. `main-view.fxml` define los controles y enlaza sus eventos con
   `MainController`.
4. `MainController` coordina las acciones del usuario y ejecuta el cifrado en
   segundo plano.
5. `FileService` valida las rutas y administra los flujos y el archivo de
   salida.
6. `CryptoService` genera la contraseña y realiza el cifrado criptográfico.

## Recorrido de una operación de cifrado

1. El usuario elige un archivo con **Seleccionar archivo** o lo arrastra al
   campo de origen. El controlador guarda la ruta absoluta.
2. El usuario elige una carpeta con **Seleccionar destino**.
3. Al pulsar **Cifrar archivo**, el controlador convierte las rutas de texto a
   `Path` y comprueba que ambas estén presentes.
4. Se deshabilitan los controles que podrían modificar las rutas y se inicia
   una `Task` JavaFX en un hilo de trabajo. Así se evita bloquear la ventana
   mientras se procesa un archivo grande.
5. `FileService` valida que el origen sea un fichero legible y que el destino
   sea una carpeta escribible. Crea un temporal dentro de esa carpeta.
6. `CryptoService` genera una contraseña, crea una cabecera R2AE y cifra el
   contenido por bloques.
7. Si la operación finaliza correctamente, `FileService` mueve el temporal al
   nombre final `<nombre-original>.enc`, sin reemplazar un archivo existente.
8. El controlador vuelve a habilitar los controles, presenta la contraseña
   oculta y muestra la ruta resultante. Si ocurre un error, limpia la contraseña
   y presenta un aviso.

## Clases de producción

### `Launcher`

**Archivo:** `src/main/java/com/enigma/javafx/Launcher.java`

Es el punto de entrada convencional de Java. No construye controles ni contiene
reglas de cifrado: entrega el ciclo de vida a la clase JavaFX principal.

#### Función `main(String[] args)`

Llama a `Application.launch(MainApplication.class, args)`. JavaFX crea la
aplicación y más adelante invoca `MainApplication.start(...)`. Los argumentos
recibidos se reenvían al mecanismo de lanzamiento de JavaFX.

La separación entre `Launcher` y `MainApplication` permite que el manifiesto
del JAR apunte a una clase sencilla, en lugar de directamente a una clase que
hereda de `Application`.

### `MainApplication`

**Archivo:** `src/main/java/com/enigma/javafx/MainApplication.java`

Extiende `javafx.application.Application`; configura la ventana principal.

#### Función `start(Stage stage)`

1. Construye un `FXMLLoader` con el recurso `main-view.fxml`.
2. Carga el árbol de controles y crea el controlador declarado en el FXML.
3. Crea una `Scene` y añade `styles.css`.
4. Asigna el título **Enigma**, coloca la escena en el `Stage` y muestra la
   ventana.

La función declara `throws IOException`: si falta el FXML o no se pueden leer
los recursos, el error no se transforma en una ventana que parezca haber
arrancado correctamente.

### `MainController`

**Archivo:** `src/main/java/com/enigma/javafx/controller/MainController.java`

Es el intermediario entre la vista y los servicios. Sus campos `@FXML` se
conectan con los elementos que tienen el mismo `fx:id` en `main-view.fxml`.
Los métodos de evento se conectan mediante atributos `onAction`.

#### Controles y estado

- `DRAG_OVER`: pseudoestado CSS para resaltar el campo mientras se arrastra un
  fichero válido.
- `fileService`: servicio usado por el controlador para cifrar.
- `encryptionTask`: referencia a la tarea activa para impedir un segundo
  cifrado simultáneo.
- `sourcePathField` y `destinationDirectoryField`: rutas de entrada y salida.
- `referencePathField`: ruta del original de referencia; se reserva para la
  futura comprobación.
- `generatedPasswordField` y `visibleGeneratedPasswordField`: dos
  representaciones enlazadas del mismo valor, una enmascarada y otra visible.
- `showGeneratedPasswordToggle`: alterna qué representación se ve.
- `decryptPasswordField` y `visibleDecryptPasswordField`: controles que la
  interfaz reserva para el descifrado futuro.
- `encryptButton`, `chooseSourceButton`, `chooseDestinationButton`,
  `copyPasswordButton` y `clearSelectionButton`: controles que se bloquean
  mientras se ejecuta el cifrado.
- `statusLabel`: mensajes de selección, progreso, resultado y error.

#### Funciones de inicialización y selección

##### `initialize()`

JavaFX la llama después de inyectar los controles FXML. Marca los campos de
contraseña generada como no editables y configura el botón de mostrar/ocultar.
Las propiedades de texto, visibilidad y espacio ocupado se enlazan para que
solo se muestre uno de los dos campos.

También registra los tres eventos de arrastre:

- **Arrastre sobre el campo:** comprueba el contenido y acepta la operación
  `COPY` solo si contiene un fichero normal único. Actualiza el pseudoestado que
  utiliza CSS para el resaltado.
- **Salida del campo:** quita el resaltado.
- **Soltar:** vuelve a validar el contenido; si es válido, usa el mismo método
  que el selector de archivos. Si no, muestra un mensaje. Finalmente comunica a
  JavaFX si la operación de soltar se completó.

##### `chooseSourceFile()`

Crea un `FileChooser`, le asigna el título y trata de abrirlo en una carpeta
relacionada con las rutas actuales. Si el usuario elige un fichero, llama a
`setSourceFile`. Si cancela, no borra ni modifica la selección anterior.

##### `chooseDestinationDirectory()`

Abre un `DirectoryChooser`. Si se selecciona una carpeta, guarda su ruta
absoluta en `destinationDirectoryField` y actualiza el estado. Cancelar mantiene
la carpeta previa.

##### `chooseReferenceFile()`

Permite seleccionar un fichero original y guarda su ruta en
`referencePathField`. Solo está implementada la selección: aún no se usa para
comparar el resultado de un descifrado.

##### `setSourceFile(File file)`

Centraliza el resultado de seleccionar o arrastrar un fichero: escribe su ruta
absoluta en el campo de entrada y comunica la selección en `statusLabel`.

##### `hasSingleRegularFile(Dragboard dragboard)`

Devuelve `true` únicamente si el `Dragboard` contiene ficheros, contiene
exactamente uno y ese elemento es un fichero normal (`File.isFile()`). Rechaza
carpetas y selecciones con varios elementos.

##### `setInitialDirectory(FileChooser, TextField...)`
##### `setInitialDirectory(DirectoryChooser, TextField...)`

Son dos sobrecargas: ambas piden a `findInitialDirectory` una carpeta existente
y, si la encuentran, la asignan al selector correspondiente. Esto hace que los
diálogos empiecen cerca de la ruta que el usuario ya estaba utilizando.

##### `findInitialDirectory(TextField...)`

Recorre los campos en el orden indicado. Ignora los vacíos. Si el texto es una
carpeta existente, la devuelve; si es un fichero o una ruta que todavía no
existe, intenta usar su carpeta padre. Si no encuentra una carpeta válida,
devuelve `null` y el diálogo utiliza su ubicación predeterminada.

##### `getOwnerWindow(Node node)`

Obtiene la ventana asociada al control, para que los diálogos de selección
pertenezcan a la ventana principal. Si el nodo todavía no forma parte de una
escena, devuelve `null`.

##### `selectedPath(TextField field)`

Convierte el contenido de un campo en `Path`. Devuelve `null` si está vacío o
solo contiene espacios; una ruta mal formada produce `InvalidPathException`,
que el flujo de cifrado captura para informar al usuario.

#### Funciones de interacción y operaciones

##### `clearSelection()`

Limpia las rutas de entrada, destino y referencia, además de las contraseñas.
También restablece y deshabilita el botón de revelar la contraseña generada.
Solo modifica la pantalla: no borra, mueve ni altera archivos del usuario.

##### `encryptFile()`

Es la función de coordinación principal:

1. Ignora la solicitud si ya hay una tarea de cifrado activa.
2. Convierte las rutas a `Path`; informa si la sintaxis de alguna ruta no es
   válida o si falta una selección.
3. Limpia el resultado de un cifrado anterior y deshabilita los controles
   relevantes.
4. Crea una `Task<EncryptionResult>` para realizar el trabajo fuera del hilo
   gráfico.
5. Enlaza el mensaje de la tarea con `statusLabel` y registra qué hacer en
   éxito, error o cancelación.
6. Arranca la tarea en un hilo daemon llamado
   `enigma-aes-encryption`.

En caso de éxito se muestra la contraseña generada, todavía enmascarada, se
habilita su botón de visibilidad y se presenta la ruta del `.enc`. En caso de
fallo se limpia la contraseña, se reactivan los controles y se muestra el
motivo del error en un `Alert`.

**Fragmento de la tarea asíncrona:**

```java
encryptionTask = new Task<>() {
    @Override
    protected EncryptionResult call() throws IOException, GeneralSecurityException {
        long totalBytes = Files.size(source);
        updateMessage("Preparando cifrado...");
        EncryptionResult result = fileService.encrypt(source, destination, processed -> {
            long percentage = totalBytes == 0 ? 100
                    : Math.min(100, Math.round(processed * 100.0 / totalBytes));
            updateMessage("Cifrando archivo: " + percentage + "%");
        });
        updateMessage("Cifrado completado.");
        return result;
    }
};
```

`Task.call()` se ejecuta en el hilo de trabajo. `FileService` comunica bytes
procesados mediante el callback `processed`; la tarea calcula el porcentaje y
actualiza su mensaje. La condición para `totalBytes == 0` evita dividir por
cero. En ficheros vacíos, la operación se considera completada al 100 %.

La interfaz escucha el resultado con `setOnSucceeded`, `setOnFailed` y
`setOnCancelled`. JavaFX entrega estos eventos en su hilo de interfaz, por lo
que los controles se actualizan de forma segura sin manipularlos desde el hilo
de cifrado.

##### `copyGeneratedPassword()`

Comprueba que exista una contraseña no vacía, la copia al portapapeles del
sistema con `Clipboard` y `ClipboardContent`, y actualiza el estado. Si todavía
no se ha cifrado un archivo, explica que primero hay que cifrarlo.

##### `showHelp()` y `showAbout()`

Delegan en `showNotImplemented(...)`; no abren todavía documentación ni una
ficha informativa de la aplicación.

##### `decryptFile()`

También delega en `showNotImplemented(...)`. En esta versión no descifra
contenedores, aunque el botón y los campos estén presentes en la interfaz.

##### `showNotImplemented(String action)`

Crea un `Alert` de tipo informativo con el título **Enigma**, indica qué acción
está pendiente y muestra el mensaje de que aún no está implementada.

#### Funciones de soporte del cifrado

##### `setEncryptionControlsDisabled(boolean disabled)`

Habilita o deshabilita conjuntamente el botón de cifrado, los selectores de
origen y destino, el botón de copiar contraseña y el botón de limpiar. Evita
que el usuario cambie las rutas mientras la tarea usa esas rutas.

##### `showError(String message)`

Presenta los errores de operación en un diálogo de alerta. Se usa para informar
de rutas inválidas, selecciones incompletas y fallos de cifrado.

## `CryptoService`

**Archivo:** `src/main/java/com/enigma/javafx/service/CryptoService.java`

Servicio criptográfico independiente de JavaFX. Genera contraseñas y cifra
flujos de entrada a salida con **AES-256-GCM**. Utiliza **PBKDF2-HMAC-SHA256**
para derivar la clave a partir de la contraseña. No implementa descifrado en el
código actual.

#### Constantes principales

- `PASSWORD_LENGTH = 16`: número de caracteres de la contraseña automática.
- `PBKDF2_ITERATIONS = 200_000`: iteraciones usadas al derivar la clave.
- `HEADER_LENGTH = 37`: tamaño de la cabecera R2AE v1.
- `SALT_LENGTH = 16`: bytes aleatorios de sal.
- `NONCE_LENGTH = 12`: nonce de GCM.
- `TAG_LENGTH_BITS = 128`: tamaño de la etiqueta de autenticación GCM.
- `AES_KEY_LENGTH_BITS = 256`: tamaño de la clave derivada.
- `BUFFER_SIZE = 64 * 1024`: bloque de lectura para el cifrado por streaming.

#### Constructores

- `CryptoService()`: crea un `SecureRandom` predeterminado.
- `CryptoService(SecureRandom secureRandom)`: recibe una fuente aleatoria;
  permite controlar la dependencia en pruebas dentro del paquete.

#### Función `generatePassword()`

Genera 16 caracteres seleccionados con `SecureRandom` de un alfabeto
alfanumérico (`A-Z`, `a-z`, `0-9`). Construye un `String` para devolverlo y
limpia con ceros el arreglo temporal `char[]` en un bloque `finally`.

El `String` devuelto es inmutable porque la interfaz necesita mostrar y copiar
la contraseña. Por eso el arreglo que se limpia es la copia temporal, no el
`String` resultante.

#### Sobrecargas de `encrypt(...)`

- `encrypt(InputStream, OutputStream, char[])`: versión sencilla, sin callback
  de progreso. Delega en la otra sobrecarga con una función vacía.
- `encrypt(InputStream, OutputStream, char[], LongConsumer)`: valida los
  argumentos, crea sal y nonce nuevos, deriva la clave, inicializa GCM, escribe
  la cabecera autenticada y cifra los datos por bloques.

La versión con callback rechaza flujos, callback o contraseña nulos, y también
una contraseña vacía, mediante `IllegalArgumentException`. Declara
`IOException` para errores de lectura/escritura y `GeneralSecurityException`
para errores de configuración criptográfica.

**Fragmento de la cabecera y asociación de datos:**

```java
byte[] header = ByteBuffer.allocate(HEADER_LENGTH)
        .order(ByteOrder.BIG_ENDIAN)
        .put(MAGIC)
        .put(FORMAT_VERSION)
        .putInt(PBKDF2_ITERATIONS)
        .put(salt)
        .put(nonce)
        .array();

Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
        new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
cipher.updateAAD(header);
output.write(header);
```

`ByteBuffer` construye la cabecera en un orden fijo. `updateAAD(header)` le
indica a GCM que autentique también esos metadatos aunque no los cifre. Si se
altera la cabecera, una implementación compatible de descifrado debe rechazar
la autenticación. La contraseña no se guarda dentro del contenedor.

Al terminar el bloque criptográfico, `finally` sobrescribe con ceros el arreglo
de bytes de la clave derivada para reducir el tiempo que permanece en memoria.

#### Función privada `deriveKey(char[] password, byte[] salt)`

Crea un `PBEKeySpec` con la contraseña, sal, iteraciones y longitud de clave
configuradas. Solicita a `SecretKeyFactory` el algoritmo
`PBKDF2WithHmacSHA256` y devuelve los bytes de la clave. En `finally` llama a
`clearPassword()` para limpiar la copia interna de contraseña mantenida por
`PBEKeySpec`.

#### Función privada `encryptStream(...)`

Lee el `InputStream` en bloques de hasta 64 KiB, pasa cada bloque a
`Cipher.update`, escribe los bytes cifrados que haya producido e invoca el
callback con el total acumulado de bytes originales procesados.

Si una lectura informa cero bytes, lee un byte individual antes de continuar.
Esto evita depender de que todos los tipos de flujo devuelvan siempre un bloque
positivo o el final del flujo.

Al final llama a `Cipher.doFinal()`. En GCM esta llamada produce los bytes
pendientes y la etiqueta de autenticación, que también se escriben en el flujo
de salida.

## `FileService`

**Archivo:** `src/main/java/com/enigma/javafx/service/FileService.java`

Se ocupa de la parte de sistema de archivos. Comprueba las rutas, conecta los
flujos del archivo con `CryptoService`, evita reemplazar resultados y publica
el archivo cifrado solo si el proceso termina correctamente.

#### Constructores

- `FileService()`: crea un `CryptoService` predeterminado.
- `FileService(CryptoService cryptoService)`: constructor de paquete que
  permite inyectar la dependencia criptográfica, especialmente útil para
  pruebas.

#### Sobrecargas de `encrypt(...)`

- `encrypt(Path source, Path destinationDirectory)`: entrada simple; delega en
  la versión con callback de progreso y un callback vacío.
- `encrypt(Path source, Path destinationDirectory, LongConsumer processedBytes)`:
  valida las rutas, genera el nombre de salida
  `<nombre-de-origen>.enc`, rechaza una salida existente, crea el temporal,
  cifra y publica el resultado. Devuelve un `EncryptionResult`.

Antes de crear nada llama a `validateSource` y
`validateDestinationDirectory`. Después comprueba `Files.exists(outputFile)` y
lanza `FileAlreadyExistsException` si el nombre final ya está ocupado.

**Fragmento de publicación segura del resultado:**

```java
Path temporaryFile = Files.createTempFile(outputDirectory, ".r2ae-", ".part");
boolean published = false;
char[] passwordChars = null;
try {
    String password = cryptoService.generatePassword();
    passwordChars = password.toCharArray();
    try (InputStream input = Files.newInputStream(inputFile);
         OutputStream output = Files.newOutputStream(temporaryFile)) {
        cryptoService.encrypt(input, output, passwordChars, processedBytes);
    }
    Files.move(temporaryFile, outputFile);
    published = true;
    return new EncryptionResult(outputFile, password);
} finally {
    if (passwordChars != null) {
        Arrays.fill(passwordChars, '\0');
    }
    if (!published) {
        Files.deleteIfExists(temporaryFile);
    }
}
```

El archivo cifrado se escribe primero en un temporal de la misma carpeta. El
`try-with-resources` cierra ambos flujos incluso si la operación falla.
`Files.move` publica el nombre definitivo solo después de completar y cerrar el
cifrado. La bandera `published` permite que `finally` distinga un resultado
terminado de una operación interrumpida: si no se publicó, intenta borrar el
temporal. La contraseña se pasa a criptografía como arreglo mutable y esa copia
se limpia al salir.

#### Función privada `validateSource(Path source)`

Rechaza `null`, convierte la ruta en absoluta y normalizada, y comprueba que
exista, sea un fichero regular y pueda leerse. Si no cumple, lanza `IOException`
con un mensaje dirigido al usuario.

#### Función privada `validateDestinationDirectory(Path destinationDirectory)`

Rechaza `null`, normaliza la ruta y verifica que exista una carpeta escribible.
Si el destino no es válido, lanza `IOException`.

#### Registro `EncryptionResult(Path outputPath, String password)`

Transporta el resultado del cifrado al controlador:

- `outputPath`: ruta del contenedor publicado.
- `password`: contraseña que permite descifrarlo y que la aplicación muestra
  una vez terminado el cifrado.

El registro es un contenedor de datos; no contiene lógica de cifrado.

## Interfaz FXML y estilos

### `main-view.fxml`

**Archivo:** `src/main/resources/com/enigma/javafx/main-view.fxml`

Define la jerarquía visual con un `BorderPane`:

- **Parte superior:** título de la aplicación y acciones Ayuda/Acerca de.
- **Centro:** un `ScrollPane` con tarjetas para selección de archivos,
  cifrado, descifrado y vista previa.
- **Parte inferior:** etiqueta de estado e indicadores visuales.

El atributo `fx:controller` señala `MainController`. Los atributos `fx:id`
vinculan controles con sus campos Java, y los atributos `onAction` asocian
botones con funciones del controlador. Por ejemplo,
`onAction="#chooseSourceFile"` llama a `chooseSourceFile()`.

Algunos controles son preparatorios: la tarjeta de descifrado, la comparación
con referencia, la vista previa, la barra y el indicador de progreso están en
la vista, pero no todos están conectados a lógica funcional. En concreto, el
progreso actual se comunica mediante el texto de estado; no se actualiza
numéricamente la `ProgressBar` del FXML.

### `styles.css`

**Archivo:** `src/main/resources/com/enigma/javafx/styles.css`

Define colores, tipografía, tarjetas, campos, botones, barra de estado y
variantes de tema oscuro. Las clases nombradas en FXML mediante `styleClass`
seleccionan estas reglas. El selector `.drop-target:drag-over` y su variante
oscura muestran el resaltado cuando el controlador activa el pseudoestado
`drag-over`.

CSS solo modifica la presentación: no implementa selección, cifrado ni
descifrado.

### `pom.xml`

Declara Maven como sistema de construcción, Java 25 como versión de compilación,
JavaFX 25 para controles y FXML, y JUnit 5.12.1 para pruebas. Configura los
plugins de compilación, ejecución JavaFX, pruebas y empaquetado con Shade para
crear un JAR con dependencias.

## Clases de prueba

Las pruebas están en `src/test/java`. Comprueban el comportamiento y las reglas
de los servicios y del controlador; no añaden funciones a la aplicación.

### `CryptoServiceTest`

Prueba `CryptoService` con datos en memoria:

- `generatesSixteenCharacterAlphanumericPasswords`: longitud y alfabeto de la
  contraseña.
- `encryptsEmptyBinaryAndUtf8ContentIntoR2aeContainers`: cabecera, versión,
  iteraciones y tamaño para contenido vacío, binario y UTF-8.
- `usesFreshSaltAndNonceForEveryEncryption`: valores distintos de sal/nonce en
  operaciones separadas.
- `reportsProcessedBytesWhileEncrypting`: cantidad de bytes comunicada por el
  callback.
- `rejectsMissingStreamsCallbacksAndPasswords`: rechazo de argumentos
  incompletos.
- `encrypt(byte[] content)`: auxiliar privado que cifra bytes en memoria y
  devuelve el contenedor para las aserciones.

### `FileServiceTest`

Utiliza una carpeta temporal JUnit aislada para probar el acceso al disco:

- `encryptsRequiredFileSizesWithoutChangingTheOriginal`: tamaños de 0 bytes,
  1 byte, 1 MiB y 20 MiB; verifica nombre, tamaño, contraseña y conservación
  del hash del original.
- `doesNotOverwriteAnExistingEncryptedFile`: comprueba que un segundo cifrado
  no reemplaza el resultado y no deja temporales.
- `rejectsMissingSourceOrDestination`: rutas que no existen.
- `rejectsNullAndWrongTypePaths`: valores nulos y rutas de tipos incorrectos.
- `removesTemporaryOutputWhenProgressCallbackFails`: garantiza la limpieza del
  temporal al interrumpirse el callback.
- `writePattern(Path file, int size)`: genera datos deterministas por bloques
  para probar ficheros grandes.
- `sha256(Path file)`: calcula SHA-256 leyendo el fichero en bloques, sin
  cargarlo entero en memoria.

### `MainControllerTest`

Prueba el controlador y el arranque de la ventana en el hilo de JavaFX:

- `startJavaFxToolkit`: inicia el toolkit una sola vez y permite que siga
  disponible durante las pruebas.
- `initializesPasswordVisibilityAndClearsAllInputFields`: verifica la
  visibilidad inicial de la contraseña y que limpiar borre los campos.
- `selectsSourceFileAndFindsExistingDirectoriesFromPathFields`: verifica la
  carga de un origen y la búsqueda de carpetas válidas para los selectores.
- `convertsOptionalPathsAndReportsMissingOwnerWindow`: prueba rutas vacías y
  rutas seleccionadas, y el caso de un control sin ventana propietaria.
- `enablesAndDisablesEncryptionControls`: comprueba el bloqueo y la
  rehabilitación de los botones durante la tarea.
- `encryptsSelectedFileAndDisplaysGeneratedPassword`: prueba el flujo
  completo de cifrado desde el controlador.
- `encryptsEmptyFileAndReportsSuccessfulCompletion`: comprueba el cifrado de
  un archivo vacío y el tamaño esperado del contenedor.
- `reportsEncryptionFailureAndPreservesAnExistingResult`: verifica el fallo
  cuando el resultado ya existe y que no se altere el archivo previo.
- `clearingPasswordCopyAttemptExplainsThatEncryptionIsNeeded`: comprueba el
  mensaje al intentar copiar sin haber cifrado.
- `copiesGeneratedPasswordToSystemClipboard`: valida la copia al portapapeles.
- `configuresBothChooserTypesFromTheCurrentFileLocation`: comprueba la carpeta
  inicial para selectores de archivo y de carpeta.
- `displaysInformationalMessagesForUnimplementedActions`: cubre los avisos de
  Ayuda, Acerca de y Descifrar.
- `reportsInvalidPathsWithoutStartingEncryption`: verifica que una ruta mal
  formada produce un aviso y no inicia la tarea.
- `startsApplicationAndLoadsItsFxmlAndStylesheet`: inicia la aplicación y
  verifica el título, la escena y la hoja de estilos.
- `invokeWhileDismissingAlert`: auxiliar para cerrar alertas durante las
  pruebas de forma programada.
- `loadController`: carga el FXML con `FXMLLoader` y devuelve su controlador.
- `assertControlsDisabled`: verifica el estado habilitado/deshabilitado de
  los controles relevantes.
- `runOnFxThread`: ejecuta una acción en el hilo de JavaFX y espera su
  resultado mediante `CompletableFuture` con límite de tiempo.
- `invoke` y `field`: auxiliares de reflexión para invocar funciones privadas y
  consultar campos privados en pruebas. La reflexión se limita al código de
  pruebas.

## Formato R2AE v1

El contenedor producido por `CryptoService` tiene esta estructura:

| Campo | Tamaño | Descripción |
|---|---:|---|
| Firma `R2AE` | 4 bytes | Identifica el formato |
| Versión | 1 byte | Valor `1` |
| Iteraciones PBKDF2 | 4 bytes | Entero big-endian |
| Sal | 16 bytes | Aleatoria por cifrado |
| Nonce | 12 bytes | Aleatorio por cifrado |
| Datos cifrados | Variable | Contenido original cifrado con AES-GCM |
| Etiqueta GCM | 16 bytes | Verificación de autenticidad e integridad |

Los primeros cinco campos suman los 37 bytes de `HEADER_LENGTH`. El contenido
se cifra como flujo; no se interpreta como texto, por lo que también se
preservan los bytes de archivos binarios. La contraseña no se incluye en el
contenedor: debe conservarse aparte.

## Compilar, ejecutar y probar

Se requiere el JDK 25 declarado por el proyecto y Maven instalado.

Desde esta carpeta (`javafx/`):

```powershell
mvn test
mvn javafx:run
mvn package
```

- `mvn test`: compila y ejecuta las pruebas.
- `mvn javafx:run`: inicia la aplicación JavaFX.
- `mvn package`: compila y empaqueta el JAR; el nombre final configurado es
  `Enigma-GestorAES-Java25.jar`.

Si Maven informa `release version 25 not supported`, el JDK seleccionado para
ejecutar Maven es anterior al 25. Comprueba `java --version` y configura
`JAVA_HOME` para que apunte al JDK requerido antes de repetir la compilación.
