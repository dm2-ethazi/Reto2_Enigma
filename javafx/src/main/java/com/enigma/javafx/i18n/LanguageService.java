package com.enigma.javafx.i18n;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Provides translations for the application interface and its user messages. */
public final class LanguageService {
    public enum Language {
        SPANISH("Español"),
        ENGLISH("English"),
        BASQUE("Euskara");

        private final String displayName;

        Language(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final Map<String, String> ENGLISH = new HashMap<>();
    private static final Map<String, String> BASQUE = new HashMap<>();

    static {
        add("RETO 02 · ENIGMA", "CHALLENGE 02 · ENIGMA", "02. ERRONKA · ENIGMA");
        add("Gestor Enigma AES", "Enigma AES Manager", "Enigma AES kudeatzailea");
        add("Cifra y recupera archivos localmente con AES-256-GCM.",
                "Encrypt and recover files locally with AES-256-GCM.",
                "Zifratu eta berreskuratu fitxategiak lokalean AES-256-GCM bidez.");
        add("GESTOR ENIGMA AES", "ENIGMA AES MANAGER", "ENIGMA AES KUDEATZAILEA");
        add("¿Qué deseas hacer?", "What would you like to do?", "Zer egin nahi duzu?");
        add("Elige una operación para continuar.", "Choose an operation to continue.",
                "Aukeratu eragiketa bat jarraitzeko.");
        add("CIFRADO AES", "AES ENCRYPTION", "AES ZIFRATZEA");
        add("DESCIFRADO AES", "AES DECRYPTION", "AES DESZIFRATZEA");
        add("Abrir la sección de cifrado AES", "Open the AES encryption section",
                "Ireki AES zifratze-atala");
        add("Abrir la sección de descifrado AES", "Open the AES decryption section",
                "Ireki AES deszifratze-atala");
        add("Protege un archivo y genera una contraseña segura",
                "Protect a file and generate a secure password",
                "Babestu fitxategi bat eta sortu pasahitz segurua");
        add("Recupera el archivo original desde un contenedor .enc",
                "Recover the original file from an .enc container",
                "Berreskuratu jatorrizko fitxategia .enc edukiontzi batetik");
        add("← Atrás", "← Back", "← Atzera");
        add("Cifrado AES", "AES encryption", "AES zifratzea");
        add("Selecciona cualquier archivo, define el destino y guarda la contraseña generada.",
                "Choose any file and its destination, then save the generated password.",
                "Aukeratu edozein fitxategi eta helburua, eta gorde sortutako pasahitza.");
        add("Selecciona cualquier archivo", "Choose any file", "Aukeratu edozein fitxategi");
        add("Fichero de origen. También puedes arrastrar y soltar un archivo aquí.",
                "Source file. You can also drag and drop a file here.",
                "Jatorrizko fitxategia. Fitxategi bat arrastatu eta jaregin dezakezu hemen.");
        add("Suelta aquí un archivo o selecciona uno", "Drop a file here or choose one",
                "Arrastatu fitxategi bat hona edo aukeratu");
        add("Se admiten archivos de texto, imágenes y otros binarios. Los originales se conservan y no se sobrescriben resultados existentes.",
                "Text files, images and other binary files are supported. Originals are kept and existing results are not overwritten.",
                "Testu-fitxategiak, irudiak eta beste fitxategi bitar batzuk onartzen dira. Jatorrizkoak gordetzen dira eta lehendik dauden emaitzak ez dira gainidazten.");
        add("Selecciona un archivo normal y legible.",
                "Choose a regular, readable file.",
                "Aukeratu fitxategi arrunt eta irakurgarri bat.");
        add("Arrastra un único archivo normal y legible.",
                "Drop a single regular, readable file.",
                "Arrastatu fitxategi arrunt eta irakurgarri bakarra.");
        add("Contenedores Enigma (*.enc)", "Enigma containers (*.enc)",
                "Enigma edukiontziak (*.enc)");
        add("Arrastra un único contenedor .enc.", "Drop a single .enc container.",
                "Arrastatu .enc edukiontzi bakarra.");
        add("Todos los archivos", "All files", "Fitxategi guztiak");
        add("Selecciona el archivo, define el destino y guarda la contraseña generada.",
                "Choose the file and destination, then save the generated password.",
                "Aukeratu fitxategia eta helburua, eta gorde sortutako pasahitza.");
        add("Archivo de origen", "Source file", "Jatorrizko fitxategia");
        add("Elige el archivo que quieres proteger y la carpeta de destino.",
                "Choose the file you want to protect and its destination folder.",
                "Aukeratu babestu nahi duzun fitxategia eta helburuko karpeta.");
        add("Contraseña segura", "Secure password", "Pasahitz segurua");
        add("Se genera una contraseña segura de 16 caracteres para proteger el archivo.",
                "A secure 16-character password is generated to protect the file.",
                "16 karaktereko pasahitz segurua sortzen da fitxategia babesteko.");
        add("Guarda la contraseña por separado: será necesaria para recuperar el archivo.",
                "Store the password separately; it is required to recover the file.",
                "Gorde pasahitza aparte; fitxategia berreskuratzeko beharrezkoa da.");
        add("Descifrado AES", "AES decryption", "AES deszifratzea");
        add("Introduce el contenedor y su contraseña para recuperar el archivo.",
                "Enter the container and its password to recover the file.",
                "Sartu edukiontzia eta haren pasahitza fitxategia berreskuratzeko.");
        add("Datos del contenedor", "Container details", "Edukiontziaren datuak");
        add("Selecciona el contenedor .enc y la carpeta donde guardar el archivo recuperado.",
                "Choose the .enc container and the folder where the recovered file will be saved.",
                "Aukeratu .enc edukiontzia eta berreskuratutako fitxategia gordetzeko karpeta.");
        add("Carpeta del archivo recuperado", "Folder for the recovered file",
                "Berreskuratutako fitxategiaren karpeta");
        add("Manual de uso local", "Local user guide", "Tokiko erabilera-gida");
        add("Atajos de teclado:", "Keyboard shortcuts:", "Teklatuko lasterbideak:");
        add("F1: abrir esta ayuda.", "F1: open this help.", "F1: ireki laguntza hau.");
        add("F2: mostrar información acerca de la aplicación.",
                "F2: show information about the application.",
                "F2: erakutsi aplikazioari buruzko informazioa.");
        add("Ctrl+1: abrir la vista de cifrado.", "Ctrl+1: open the encryption view.",
                "Ctrl+1: ireki zifratze-ikuspegia.");
        add("Ctrl+2: abrir la vista de descifrado.", "Ctrl+2: open the decryption view.",
                "Ctrl+2: ireki deszifratze-ikuspegia.");
        add("Ctrl+Enter: ejecutar la operación de la vista actual.",
                "Ctrl+Enter: run the operation in the current view.",
                "Ctrl+Enter: exekutatu uneko ikuspegiko eragiketa.");
        add("Esc: volver al menú principal.", "Esc: return to the main menu.",
                "Esc: itzuli menu nagusira.");
        add("Ayuda - Gestor Enigma AES", "Help - Enigma AES Manager",
                "Laguntza - Enigma AES kudeatzailea");
        add("Acerca de Gestor Enigma AES", "About Enigma AES Manager",
                "Enigma AES kudeatzaileari buruz");
        add("Cifrado: selecciona un archivo .txt, .png o cualquier otro archivo binario y una carpeta de destino; pulsa Cifrar archivo y guarda la contraseña generada de 16 caracteres.\n\nDescifrado: selecciona el contenedor .enc, la carpeta de salida y la contraseña; pulsa Descifrar archivo. La comparación con el original es opcional.\n\nLa vista previa se muestra después de un descifrado correcto para archivos de texto e imágenes PNG compatibles. Los archivos se procesan localmente y los resultados existentes no se sobrescriben.",
                "Encryption: choose a .txt, .png or any other binary file and a destination folder; click Encrypt file and save the generated 16-character password.\n\nDecryption: choose the .enc container, an output folder and the password; click Decrypt file. Comparing with the original is optional.\n\nThe preview appears after successful decryption for supported text and PNG files. Files are processed locally and existing results are not overwritten.",
                "Zifratzea: aukeratu .txt, .png edo beste fitxategi bat eta helburuko karpeta; sakatu Zifratu fitxategia eta gorde sortutako 16 karaktereko pasahitza.\n\nDeszifratzea: aukeratu .enc edukiontzia, irteerako karpeta eta pasahitza; sakatu Deszifratu fitxategia. Jatorrizkoarekin alderatzea aukerakoa da.\n\nAurrebista testu-fitxategi eta PNG bateragarriak behar bezala deszifratu ondoren agertzen da. Fitxategiak lokalean prozesatzen dira eta lehendik dauden emaitzak ez dira gainidazten.");
        add("Gestor Enigma AES\nAutoría: equipo Koldo, Kenneth y Carlos.\nVersión del formato de contenedor: R2AE v1.\nCifrado y autenticación de archivos con AES-256-GCM. El procesamiento se realiza localmente.",
                "Enigma AES Manager\nAuthors: Koldo, Kenneth and Carlos.\nContainer format version: R2AE v1.\nFile encryption and authentication with AES-256-GCM. Processing is performed locally.",
                "Enigma AES kudeatzailea\nEgileak: Koldo, Kenneth eta Carlos.\nEdukiontzi-formatuaren bertsioa: R2AE v1.\nFitxategien zifratzea eta autentifikazioa AES-256-GCM bidez. Prozesamendua lokalean egiten da.");
        add("Cerrar", "Close", "Itxi");
        add("Ayuda", "Help", "Laguntza");
        add("Ayuda sobre el uso de la aplicación", "Help with using the application",
                "Aplikazioa erabiltzeko laguntza");
        add("Acerca de", "About", "Honi buruz");
        add("Información de la aplicación", "Application information",
                "Aplikazioari buruzko informazioa");
        add("Idioma", "Language", "Hizkuntza");
        add("Idioma de la aplicación", "Application language", "Aplikazioaren hizkuntza");
        add("Archivos", "Files", "Fitxategiak");
        add("Elige el archivo de entrada y la carpeta de destino.",
                "Choose the input file and destination folder.",
                "Aukeratu sarrerako fitxategia eta helburuko karpeta.");
        add("Fichero de origen", "Source file", "Sarrerako fitxategia");
        add("Fichero de origen. También puedes arrastrar y soltar un fichero aquí.",
                "Source file. You can also drag and drop a file here.",
                "Jatorrizko fitxategia. Fitxategi bat arrastatu eta jaregin dezakezu hemen.");
        add("Suelta aquí un fichero o selecciona uno", "Drop a file here or choose one",
                "Arrastatu fitxategi bat hona edo aukeratu");
        add("Seleccionar archivo", "Choose file", "Aukeratu fitxategia");
        add("Carpeta destino", "Destination folder", "Helburuko karpeta");
        add("Carpeta del archivo resultante", "Folder for the output file",
                "Emaitzako fitxategiaren karpeta");
        add("Seleccionar destino", "Choose destination", "Aukeratu helburua");
        add("Limpiar selección", "Clear selection", "Garbitu hautapena");
        add("Limpiar las rutas y contraseñas de la pantalla sin borrar archivos",
                "Clear paths and passwords from the screen without deleting files",
                "Garbitu pantailako bideak eta pasahitzak fitxategiak ezabatu gabe");
        add("Ctrl+Enter cifra archivos normales y descifra contenedores .enc. Los originales se conservan y no se sobrescriben resultados existentes.",
                "Ctrl+Enter encrypts regular files and decrypts .enc containers. Originals are kept and existing results are not overwritten.",
                "Ctrl+Enter teklak fitxategi arruntak zifratzen ditu eta .enc edukiontziak deszifratzen ditu. Jatorrizkoak gordetzen dira eta lehendik dauden emaitzak ez dira gainidazten.");
        add("Cifrado (AES)", "Encryption (AES)", "Zifratzea (AES)");
        add("Genera una contraseña segura de 16 caracteres para proteger el archivo.",
                "Generate a secure 16-character password to protect the file.",
                "Sortu 16 karaktereko pasahitz segurua fitxategia babesteko.");
        add("Cifrar archivo", "Encrypt file", "Zifratu fitxategia");
        add("Contraseña generada (16 caracteres)", "Generated password (16 characters)",
                "Sortutako pasahitza (16 karaktere)");
        add("Aparecerá al terminar el cifrado", "Appears when encryption is complete",
                "Zifratzea amaitzean agertuko da");
        add("Contraseña generada para el archivo cifrado",
                "Generated password for the encrypted file",
                "Zifratutako fitxategirako sortutako pasahitza");
        add("Contraseña generada visible", "Visible generated password",
                "Sortutako pasahitza ikusgai");
        add("Mostrar contraseña generada", "Show generated password",
                "Erakutsi sortutako pasahitza");
        add("Mostrar contraseña", "Show password", "Erakutsi pasahitza");
        add("Ocultar contraseña", "Hide password", "Ezkutatu pasahitza");
        add("Ocultar contraseña generada", "Hide generated password",
                "Ezkutatu sortutako pasahitza");
        add("Copiar", "Copy", "Kopiatu");
        add("Usa A-Z, a-z y 0-9 con selección criptográficamente segura. Guarda la contraseña por separado.",
                "Uses A-Z, a-z and 0-9 with cryptographically secure selection. Store the password separately.",
                "A-Z, a-z eta 0-9 erabiltzen ditu, hautaketa kriptografikoki segurua eginez. Gorde pasahitza aparte.");
        add("Descifrado (AES)", "Decryption (AES)", "Deszifratzea (AES)");
        add("Introduce la contraseña del contenedor para recuperar exactamente los bytes originales.",
                "Enter the container password to recover the exact original bytes.",
                "Sartu edukiontziaren pasahitza jatorrizko byteak zehazki berreskuratzeko.");
        add("Archivo cifrado", "Encrypted file", "Zifratutako fitxategia");
        add("Selecciona un archivo .enc", "Choose an .enc file", "Aukeratu .enc fitxategi bat");
        add("Contraseña", "Password", "Pasahitza");
        add("Contraseña del archivo .enc", "Password for the .enc file",
                ".enc fitxategiaren pasahitza");
        add("Contraseña del archivo cifrado", "Password for the encrypted file",
                "Zifratutako fitxategiaren pasahitza");
        add("Contraseña visible del archivo cifrado", "Visible password for the encrypted file",
                "Zifratutako fitxategiaren pasahitza ikusgai");
        add("Descifrar archivo", "Decrypt file", "Deszifratu fitxategia");
        add("Comparar con el archivo original", "Compare with the original file",
                "Konparatu jatorrizko fitxategiarekin");
        add("Comparar el archivo recuperado con el archivo original",
                "Compare the recovered file with the original file",
                "Konparatu berreskuratutako fitxategia jatorrizkoarekin");
        add("Archivo original", "Original file", "Jatorrizko fitxategia");
        add("Opcional: selecciona el original", "Optional: choose the original",
                "Aukerakoa: aukeratu jatorrizkoa");
        add("Seleccionar original", "Choose original", "Aukeratu jatorrizkoa");
        add("Resultado de la comparación", "Comparison result", "Konparazioaren emaitza");
        add("El archivo descifrado coincide con el original.",
                "The decrypted file matches the original.",
                "Deszifratutako fitxategia jatorrizkoarekin bat dator.");
        add("La comparación byte a byte ha confirmado que ambos archivos son iguales.",
                "A byte-for-byte comparison confirmed that both files are identical.",
                "Bytez byteko konparazioak bi fitxategiak berdinak direla baieztatu du.");
        add("Cerrar", "Close", "Itxi");
        add("La etiqueta GCM valida el contenedor. La comparación opcional comprueba además tamaño y SHA-256.",
                "The GCM tag validates the container. Optional comparison also checks size and SHA-256.",
                "GCM etiketak edukiontzia baliozkotzen du. Aukerako konparazioak tamaina eta SHA-256 ere egiaztatzen ditu.");
        add("Vista previa", "Preview", "Aurrebista");
        add("El contenido se mostrará aquí después de pulsar Descifrar archivo.",
                "The contents will appear here after you click Decrypt file.",
                "Edukia hemen agertuko da Deszifratu fitxategia sakatu ondoren.");
        add("Texto recuperado", "Recovered text", "Berreskuratutako testua");
        add("El texto descifrado aparecerá aquí.", "Decrypted text will appear here.",
                "Deszifratutako testua hemen agertuko da.");
        add("Vista previa del texto descifrado", "Preview of decrypted text",
                "Deszifratutako testuaren aurrebista");
        add("Imagen recuperada", "Recovered image", "Berreskuratutako irudia");
        add("Vista previa de la imagen PNG descifrada", "Preview of the decrypted PNG image",
                "Deszifratutako PNG irudiaren aurrebista");
        add("La vista previa es limitada; la operación AES conserva siempre el archivo completo sin transformarlo.",
                "The preview is limited; AES always preserves the complete file without transforming it.",
                "Aurrebista mugatua da; AES eragiketak fitxategi osoa bere horretan gordetzen du.");
        add("La vista previa solo está disponible para archivos .txt.enc y .png.enc.",
                "Preview is only available for .txt.enc and .png.enc files.",
                "Aurrebista .txt.enc eta .png.enc fitxategietarako bakarrik dago erabilgarri.");
        add("Selecciona el contenedor .enc e introduce su contraseña para generar la vista previa.",
                "Choose the .enc container and enter its password to generate the preview.",
                "Aukeratu .enc edukiontzia eta sartu pasahitza aurrebista sortzeko.");
        add("Vista previa autenticada. Se muestran los primeros {0} bytes de {1}.",
                "Authenticated preview. Showing the first {0} bytes of {1}.",
                "Autentifikatutako aurrebista. {1} byteetatik lehenengo {0} erakusten dira.");
        add("Vista previa autenticada. Contenido completo ({0} bytes).",
                "Authenticated preview. Full contents ({0} bytes).",
                "Autentifikatutako aurrebista. Eduki osoa ({0} byte).");
        add("El PNG supera el límite de vista previa de {0} MiB; no se muestra.",
                "The PNG exceeds the {0} MiB preview limit and will not be displayed.",
                "PNGak {0} MiB-ko aurrebista-muga gainditzen du; ez da erakutsiko.");
        add("El archivo descifrado no es un PNG válido o supera el tamaño permitido.",
                "The decrypted file is not a valid PNG or exceeds the allowed dimensions.",
                "Deszifratutako fitxategia ez da PNG baliozkoa edo baimendutako tamaina gainditzen du.");
        add("Vista previa autenticada. La imagen PNG se ha cargado.",
                "Authenticated preview. The PNG image has been loaded.",
                "Autentifikatutako aurrebista. PNG irudia kargatu da.");
        add("Cargando vista previa PNG...",
                "Loading PNG preview...",
                "PNG aurrebista kargatzen...");
        add("Validando el contenedor para preparar la vista previa...",
                "Authenticating the container for preview...",
                "Edukiontzia baliozkotzen aurrebista prestatzeko...");
        add("Validando archivo para vista previa: {0}%",
                "Authenticating file for preview: {0}%",
                "Fitxategia baliozkotzen aurrebistarako: {0}%");
        add("Vista previa autenticada.", "Preview authenticated.",
                "Aurrebista autentifikatuta.");
        add("No se pudo mostrar la vista previa.", "The preview could not be displayed.",
                "Ezin izan da aurrebista erakutsi.");
        add("La vista previa se ha cancelado.", "The preview was cancelled.",
                "Aurrebista bertan behera utzi da.");
        add("No se pudo validar el contenedor para la vista previa.",
                "The container could not be authenticated for preview.",
                "Ezin izan da edukiontzia baliozkotu aurrebistarako.");
        add("Vista previa autenticada; la imagen supera el límite de tamaño.",
                "Preview authenticated; the image exceeds the size limit.",
                "Aurrebista autentifikatuta; irudiak tamaina-muga gainditzen du.");
        add("El contenido no se puede mostrar como imagen PNG.",
                "The contents cannot be displayed as a PNG image.",
                "Edukia ezin da PNG irudi gisa erakutsi.");
        add("Estado de la aplicación", "Application status", "Aplikazioaren egoera");
        add("Listo para trabajar.", "Ready.", "Prest.");
        add("Progreso de la operación", "Operation progress", "Eragiketaren aurrerapena");
        add("Porcentaje de progreso", "Progress percentage", "Aurrerapenaren ehunekoa");
        add("Procesamiento local · R2AE v1", "Local processing · R2AE v1",
                "Prozesamendu lokala · R2AE v1");

        add("Seleccionar archivo de origen", "Choose source file",
                "Aukeratu sarrerako fitxategia");
        add("Logo de Hogwarts Enigma", "Hogwarts Enigma logo", "Hogwarts Enigmaren logotipoa");
        add("Seleccionar carpeta de destino", "Choose destination folder",
                "Aukeratu helburuko karpeta");
        add("Seleccionar archivo cifrado", "Choose encrypted file",
                "Aukeratu zifratutako fitxategia");
        add("Seleccionar archivo original", "Choose original file",
                "Aukeratu jatorrizko fitxategia");
        add("Error de cifrado", "Encryption error", "Zifratze-errorea");
        add("Arrastra un único archivo válido.", "Drop exactly one valid file.",
                "Arrastatu eta jaregin baliozko fitxategi bakarra.");
        add("Carpeta de destino seleccionada.", "Destination folder selected.",
                "Helburuko karpeta aukeratu da.");
        add("Selección limpiada.", "Selection cleared.", "Hautapena garbitu da.");
        add("Preparando cifrado...", "Preparing encryption...", "Zifratzea prestatzen...");
        add("Cifrando archivo: {0}%", "Encrypting file: {0}%", "Fitxategia zifratzen: {0}%");
        add("Cifrado completado.", "Encryption complete.", "Zifratzea amaitu da.");
        add("Archivo cifrado: {0}", "Encrypted file: {0}", "Zifratutako fitxategia: {0}");
        add("No se pudo cifrar el archivo.", "The file could not be encrypted.",
                "Ezin izan da fitxategia zifratu.");
        add("Cifrado cancelado.", "Encryption cancelled.", "Zifratzea bertan behera utzi da.");
        add("Cifra un archivo antes de copiar su contraseña.",
                "Encrypt a file before copying its password.",
                "Zifratu fitxategi bat pasahitza kopiatu aurretik.");
        add("Contraseña copiada al portapapeles.", "Password copied to the clipboard.",
                "Pasahitza arbelean kopiatu da.");
        add("Preparando descifrado...", "Preparing decryption...", "Deszifratzea prestatzen...");
        add("Descifrando archivo: {0}%", "Decrypting file: {0}%", "Fitxategia deszifratzen: {0}%");
        add("Descifrado completado.", "Decryption complete.", "Deszifratzea amaitu da.");
        add("Archivo descifrado: {0}", "Decrypted file: {0}", "Deszifratutako fitxategia: {0}");
        add("No se pudo descifrar el archivo.", "The file could not be decrypted.",
                "Ezin izan da fitxategia deszifratu.");
        add("Descifrado cancelado.", "Decryption cancelled.",
                "Deszifratzea bertan behera utzi da.");
        add("Archivo cifrado seleccionado.", "Encrypted file selected.",
                "Zifratutako fitxategia aukeratu da.");
        add("Archivo original seleccionado.", "Original file selected.",
                "Jatorrizko fitxategia aukeratu da.");
        add("Archivo de entrada seleccionado.", "Input file selected.",
                "Sarrerako fitxategia aukeratu da.");
        add("Una de las rutas seleccionadas no es válida.", "One of the selected paths is invalid.",
                "Aukeratutako bideetako bat ez da baliozkoa.");
        add("Selecciona un fichero de origen y una carpeta de destino.",
                "Choose a source file and a destination folder.",
                "Aukeratu jatorrizko fitxategi bat eta helburuko karpeta bat.");
        add("Selecciona el contenedor .enc y una carpeta de destino.",
                "Choose the .enc container and a destination folder.",
                "Aukeratu .enc edukiontzia eta helburuko karpeta bat.");
        add("Introduce la contraseña del contenedor.", "Enter the container password.",
                "Sartu edukiontziaren pasahitza.");
        add("Selecciona el archivo original para poder compararlo.",
                "Choose the original file to compare it.",
                "Aukeratu jatorrizko fitxategia konparatu ahal izateko.");

        add("El archivo de destino ya existe; no se sobrescribirá.",
                "The destination file already exists and will not be overwritten.",
                "Helburuko fitxategia badago eta ez da gainidatziko.");
        add("El archivo de destino ya existe; no se sobrescribirá: {0}",
                "The destination file already exists and will not be overwritten: {0}",
                "Helburuko fitxategia badago eta ez da gainidatziko: {0}");
        add("Selecciona un fichero de origen.", "Choose a source file.",
                "Aukeratu jatorrizko fitxategi bat.");
        add("El fichero de origen no existe o no se puede leer.",
                "The source file does not exist or cannot be read.",
                "Jatorrizko fitxategia ez dago edo ezin da irakurri.");
        add("Selecciona una carpeta de destino.", "Choose a destination folder.",
                "Aukeratu helburuko karpeta bat.");
        add("La carpeta de destino no existe o no permite escribir.",
                "The destination folder does not exist or is not writable.",
                "Helburuko karpeta ez dago edo ezin da bertan idatzi.");
        add("Selecciona un contenedor cifrado con extensión .enc.",
                "Choose an encrypted container with the .enc extension.",
                "Aukeratu .enc luzapena duen edukiontzi zifratu bat.");
        add("El contenedor R2AE está incompleto.", "The R2AE container is incomplete.",
                "R2AE edukiontzia osatu gabe dago.");
        add("El archivo descifrado no coincide con el original de referencia.",
                "The decrypted file does not match the reference original.",
                "Deszifratutako fitxategia ez dator bat erreferentziako jatorrizkoarekin.");
        add("El archivo no contiene un contenedor R2AE válido.",
                "The file does not contain a valid R2AE container.",
                "Fitxategiak ez du baliozko R2AE edukiontzirik.");
        add("La versión del contenedor R2AE no es compatible.",
                "This R2AE container version is not supported.",
                "R2AE edukiontziaren bertsio hau ez da bateragarria.");
        add("El número de iteraciones del contenedor no es compatible.",
                "The iteration count in this container is not supported.",
                "Edukiontzi honetako iterazio kopurua ez da bateragarria.");
        add("La contraseña es incorrecta o el archivo cifrado está dañado.",
                "The password is incorrect or the encrypted file is damaged.",
                "Pasahitza okerra da edo fitxategi zifratua hondatuta dago.");
        add("No se pudo completar la operación de archivo. Comprueba los permisos y el espacio disponible.",
                "The file operation could not be completed. Check permissions and available space.",
                "Ezin izan da fitxategi-eragiketa osatu. Egiaztatu baimenak eta leku librea.");
        add("No se pudo completar la operación criptográfica.",
                "The cryptographic operation could not be completed.",
                "Ezin izan da eragiketa kriptografikoa osatu.");
        add("No se pudo completar la operación.", "The operation could not be completed.",
                "Ezin izan da eragiketa osatu.");
    }

    private LanguageService() {
    }

    private static void add(String spanish, String english, String basque) {
        ENGLISH.put(spanish, english);
        BASQUE.put(spanish, basque);
    }

    public static String translate(Language language, String text) {
        if (text == null) {
            return text;
        }
        String spanish = findSpanish(text);
        if (language == Language.SPANISH) {
            return spanish;
        }
        Map<String, String> translations = language == Language.ENGLISH ? ENGLISH : BASQUE;
        String translated = translations.get(spanish);
        return translated == null ? text : translated;
    }

    public static String format(Language language, String spanishPattern, Object... arguments) {
        return new MessageFormat(translate(language, spanishPattern), locale(language))
                .format(arguments);
    }

    private static String findSpanish(String text) {
        if (ENGLISH.containsKey(text) || BASQUE.containsKey(text)) {
            return text;
        }
        for (Map.Entry<String, String> entry : ENGLISH.entrySet()) {
            if (entry.getValue().equals(text) || BASQUE.get(entry.getKey()).equals(text)) {
                return entry.getKey();
            }
        }
        return text;
    }

    private static Locale locale(Language language) {
        return switch (language) {
            case SPANISH -> Locale.forLanguageTag("es");
            case ENGLISH -> Locale.ENGLISH;
            case BASQUE -> Locale.forLanguageTag("eu");
        };
    }
}
