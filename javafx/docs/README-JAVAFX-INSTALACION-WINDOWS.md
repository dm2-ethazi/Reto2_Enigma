# Instalación y ejecución en Windows

Esta guía cubre únicamente Windows. Describe cómo preparar el entorno, ejecutar
la aplicación desde el código fuente y generar el JAR empaquetado. No se
modifica el [README.md](./README.md) existente.

> **Formato de distribución actual:** el proyecto configura un JAR con
> dependencias; no configura un instalador `.exe` ni incluye un runtime de Java.
> Para ejecutarlo se necesita un JDK 25 instalado en Windows.

## Requisitos

- Windows 10 u 11 de 64 bits.
- JDK 25. Se puede instalar, por ejemplo, Eclipse Temurin 25 desde
  [Adoptium](https://adoptium.net/temurin/releases/?version=25).
- Apache Maven 3.9 o posterior desde
  [Apache Maven](https://maven.apache.org/download.cgi).
- El código fuente del repositorio.

El proyecto fija `maven.compiler.release` y JavaFX en la versión 25; JDK 21 no
es suficiente para compilarlo.

## 1. Comprobar Java y Maven

Abre PowerShell y ejecuta:

```powershell
java -version
mvn -version
```

Comprueba que Java informa la versión 25 y que Maven puede encontrar ese mismo
JDK. Si `mvn` no se reconoce, instala Maven y añade su carpeta `bin` al `PATH`.
Si Maven está usando otro Java, configura `JAVA_HOME` con la ruta real de tu
JDK 25 y abre una nueva consola. Para configurar solo la sesión actual, adapta
este ejemplo a la carpeta donde lo hayas instalado:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -version
```

## 2. Abrir la carpeta del proyecto

Clona el repositorio o descarga el código y, desde PowerShell, entra en la
carpeta `javafx`:

```powershell
Set-Location "C:\ruta\al\Reto2_Enigma\javafx"
```

La carpeta actual debe contener `pom.xml`.

## 3. Ejecutar las pruebas

Antes de ejecutar o empaquetar la aplicación, comprueba el proyecto y la
cobertura mínima de líneas del 80 %:

```powershell
mvn clean verify
```

Maven descargará las dependencias necesarias la primera vez. Si las pruebas y
la comprobación JaCoCo terminan con `BUILD SUCCESS`, continúa con la ejecución.
El informe HTML queda en `target\site\jacoco\index.html`.

## 4. Ejecutar desde el código fuente

Para iniciar la ventana JavaFX con Maven:

```powershell
mvn javafx:run
```

La ventana se cierra desde sus controles habituales. Si no arranca, conserva y
revisa el mensaje completo de PowerShell; suele indicar si Maven está usando
una versión de Java incorrecta o si falta una dependencia.

## 5. Crear y ejecutar el JAR

Desde la carpeta `javafx`, crea el paquete:

```powershell
mvn clean package
```

El artefacto configurado por el proyecto debe aparecer en:

```text
target\Enigma-GestorAES-Java25-windows.jar
```

Ejecútalo desde PowerShell:

```powershell
java -jar .\target\Enigma-GestorAES-Java25-windows.jar
```

Para probarlo en otro equipo Windows, copia el JAR y asegúrate de que allí
también esté instalado JDK 25. No copies únicamente el JAR si el equipo no tiene
Java. Este proyecto no genera actualmente un `.exe` ni un instalador con Java
incluido.

## Problemas habituales

| Síntoma | Qué comprobar |
|---|---|
| `mvn` no se reconoce | Maven está instalado y su directorio `bin` está en `PATH`; abre una nueva PowerShell. |
| Error de compilación por versión de Java | `mvn -version` debe mostrar JDK 25, no solo que `java -version` lo muestre. |
| No se encuentra `pom.xml` | La consola debe estar en `Reto2_Enigma\javafx`. |
| El JAR no aparece | `mvn clean package` terminó con `BUILD SUCCESS`; revisa `javafx\target`. |
| El JAR no inicia la interfaz | Prueba primero `mvn javafx:run` y conserva el error completo. Comprueba que se ejecuta en Windows con JDK 25. |
| No se puede escribir el resultado | Selecciona una carpeta existente con permisos de escritura y comprueba que no exista ya el archivo final. |

## Archivos de salida y contraseñas

La aplicación conserva el archivo original al cifrar y crea un contenedor `.enc`
en la carpeta elegida. Al descifrar, escribe primero un temporal `.part` y solo
lo publica como archivo final después de validar la autenticidad; si se activó
la comparación con el original, también espera a que esa comparación coincida.

Guarda la contraseña de cifrado de forma segura y separada del contenedor. Sin
ella no se podrá recuperar el contenido.

