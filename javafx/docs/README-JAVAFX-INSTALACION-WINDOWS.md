# Manual de instalación, configuración y ejecución de JavaFX 25 en Windows

Guía para Windows 11 de 64 bits, uno de los sistemas definidos para el
proyecto. El JAR incluye JavaFX y sus dependencias nativas de Windows, pero no
incluye el JDK.

## 1. Instalar JDK 25 LTS

1. Descarga JDK 25 LTS x64 desde [Eclipse Adoptium](https://adoptium.net/).
2. Ejecuta el instalador `.msi` y conserva la ruta de instalación. Por ejemplo:

   ```text
   C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
   ```

## 2. Instalar Apache Maven 3.6.3

1. Descarga `apache-maven-3.6.3-bin.zip` desde el
   [archivo oficial de Maven](https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/)
   o una versión posterior desde
   [Apache Maven](https://maven.apache.org/download.cgi).
2. Extrae el archivo en una ubicación fija, por ejemplo:

   ```text
   C:\Program Files\Apache\apache-maven-3.6.3
   ```

El proyecto requiere Maven 3.6.3 o posterior.

## 3. Configurar las variables de entorno

Para configurarlas desde la interfaz de Windows:

1. Pulsa `Win + R`, escribe `sysdm.cpl` y pulsa **Enter**.
2. Abre **Opciones avanzadas → Variables de entorno**.
3. En las variables del sistema, crea o actualiza `JAVA_HOME` con la ruta raíz
   del JDK 25, por ejemplo:

   ```text
   C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
   ```

4. Añade al `Path` las carpetas `bin` del JDK y de Maven:

   ```text
   C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin
   C:\Program Files\Apache\apache-maven-3.6.3\bin
   ```

Como alternativa, para configurar solo la ventana actual de PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Program Files\Apache\apache-maven-3.6.3\bin;$env:Path"
```

Adapta las rutas a las ubicaciones reales del equipo.

## 4. Verificar la instalación

Abre una nueva ventana de PowerShell o CMD y ejecuta:

```powershell
java -version
javac -version
mvn -version
```

Comprueba que Java y Maven utilizan JDK 25.

## 5. Clonar, empaquetar y ejecutar

Clona el repositorio y entra en el módulo JavaFX:

```powershell
git clone <URL_DEL_REPOSITORIO>
Set-Location ".\Reto2_Enigma\javafx"
```

Compila y empaqueta el proyecto:

```powershell
mvn clean package
```

Desde la carpeta `target`, ejecuta el JAR de Windows:

```powershell
Set-Location ".\target"
java -jar .\Enigma-GestorAES-Java25-windows.jar
```

Si el equipo tiene varios JDK instalados y `java` apunta a una versión
distinta de la 25, ejecuta explícitamente el `java.exe` del JDK 25 desde esa
misma carpeta `target`:

```powershell
& "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -jar Enigma-GestorAES-Java25-windows.jar
```

Si JDK 25 está instalado en otra ruta, sustituye la ruta de `java.exe` por la
correspondiente de ese equipo.

También puedes iniciar la aplicación desde la carpeta `javafx` con
`mvn javafx:run`. Para ejecutar el JAR en otro equipo, ese equipo debe tener
JDK 25 instalado. El proyecto no genera un instalador `.exe`.

