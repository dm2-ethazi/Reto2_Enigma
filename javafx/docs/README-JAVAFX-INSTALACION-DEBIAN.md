# Manual de instalación, configuración y ejecución de JavaFX 25 en Ubuntu / Debian

Esta guía se aplica a Ubuntu 26.04.1 LTS y Debian 13.1.0, los sistemas Linux
definidos para el proyecto. El JAR de Linux incluye JavaFX y sus dependencias
nativas de Linux, pero no incluye el JDK.

## 1. Instalar JDK 25 y dependencias gráficas

Abre una terminal y actualiza los repositorios:

```bash
sudo apt update
sudo apt upgrade -y
```

Instala JDK 25, Maven y las bibliotecas necesarias para JavaFX:

```bash
sudo apt install openjdk-25-jdk maven -y
sudo apt install libgtk-3-0 libgl1-mesa-dri libasound2t64 libxtst6 libx11-xcb1 -y
```

Si la distribución no ofrece `openjdk-25-jdk` en sus repositorios, instala JDK
25 x64 desde una distribución oficial, como
[Eclipse Temurin](https://adoptium.net/). No uses una versión anterior: el
proyecto requiere Java 25. Si un paquete gráfico no está disponible con ese
nombre, consulta `apt-cache search` y utiliza el nombre equivalente de la
versión instalada.

## 2. Configurar Java

Abre el archivo de perfil del usuario:

```bash
nano ~/.bashrc
```

Añade al final:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

Guarda los cambios y aplícalos en la sesión actual:

```bash
source ~/.bashrc
```

## 3. Verificar Java e instalar Git

Comprueba Java, Maven y `JAVA_HOME`:

```bash
java -version
javac -version
mvn -version
echo "$JAVA_HOME"
```

Instala y verifica Git:

```bash
sudo apt install git -y
git --version
```

## 4. Clonar, empaquetar y ejecutar

Clona el repositorio y entra en el módulo JavaFX:

```bash
git clone <URL_DEL_REPOSITORIO>
cd Reto2_Enigma/javafx
```

Compila y empaqueta el proyecto:

```bash
mvn clean package
```

Desde `target`, ejecuta el JAR de Linux:

```bash
cd target
java -jar Enigma-GestorAES-Java25-linux.jar
```

No es necesario marcar el JAR como ejecutable: se inicia mediante `java -jar`.
Construye el JAR en Linux; no reutilices el JAR de Windows.

Si ejecutas la aplicación en una máquina virtual sin aceleración OpenGL y
JavaFX no puede iniciar el renderizador, prueba el renderizado por software:

```bash
LIBGL_ALWAYS_SOFTWARE=1 java -Dprism.order=sw \
  -jar Enigma-GestorAES-Java25-linux.jar
```

Para iniciar directamente desde el código fuente, desde `javafx`, ejecuta
`mvn javafx:run`.
