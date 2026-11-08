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
        add("Escudo de Enigma Hogwarts", "Enigma Hogwarts crest", "Enigma Hogwartsen armarria");
        add("Seleccionar carpeta de destino", "Choose destination folder",
                "Aukeratu helburuko karpeta");
        add("Seleccionar archivo cifrado", "Choose encrypted file",
                "Aukeratu zifratutako fitxategia");
        add("Seleccionar archivo original", "Choose original file",
                "Aukeratu jatorrizko fitxategia");
        add("Contenedores Enigma (*.enc)", "Enigma containers (*.enc)",
                "Enigma edukiontziak (*.enc)");
        add("Contenedores Enigma (*.txt.enc, *.png.enc)",
                "Enigma containers (*.txt.enc, *.png.enc)",
                "Enigma edukiontziak (*.txt.enc, *.png.enc)");
        add("Archivos de texto e imágenes (*.txt, *.png)",
                "Text and image files (*.txt, *.png)",
                "Testu eta irudi fitxategiak (*.txt, *.png)");
        add("Error de cifrado", "Encryption error", "Zifratze-errorea");
        add("Esta función todavía no está implementada.",
                "This feature has not been implemented yet.",
                "Funtzio hau oraindik ez dago inplementatuta.");
        add("Arrastra un único archivo válido.", "Drop exactly one valid file.",
                "Arrastatu eta jaregin baliozko fitxategi bakarra.");
        add("Arrastra un único archivo .txt o .png.", "Drop exactly one .txt or .png file.",
                "Arrastatu eta jaregin .txt edo .png fitxategi bakarra.");
        add("Arrastra un único contenedor .txt.enc o .png.enc.",
                "Drop exactly one .txt.enc or .png.enc container.",
                "Arrastatu eta jaregin .txt.enc edo .png.enc edukiontzi bakarra.");
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
        add("Selecciona un archivo .txt o .png.", "Choose a .txt or .png file.",
                "Aukeratu .txt edo .png fitxategi bat.");
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
