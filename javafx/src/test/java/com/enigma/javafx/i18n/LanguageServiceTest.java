package com.enigma.javafx.i18n;

import org.junit.jupiter.api.Test;
import static com.enigma.javafx.i18n.LanguageService.Language.BASQUE;
import static com.enigma.javafx.i18n.LanguageService.Language.ENGLISH;
import static com.enigma.javafx.i18n.LanguageService.Language.SPANISH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageServiceTest {
    @Test
    void translatesInterfaceAndErrorMessagesInAllSupportedLanguages() {
        assertEquals("Cifrar archivo", LanguageService.translate(SPANISH, "Cifrar archivo"));
        assertEquals("Encrypt file", LanguageService.translate(ENGLISH, "Cifrar archivo"));
        assertEquals("Zifratu fitxategia", LanguageService.translate(BASQUE, "Cifrar archivo"));

        assertEquals("The password is incorrect or the encrypted file is damaged.",
                LanguageService.translate(ENGLISH,
                        "La contraseña es incorrecta o el archivo cifrado está dañado."));
        assertEquals("Pasahitza okerra da edo fitxategi zifratua hondatuta dago.",
                LanguageService.translate(BASQUE,
                        "La contraseña es incorrecta o el archivo cifrado está dañado."));
    }

    @Test
    void translatesTextFromAnySupportedLanguageWhenSwitchingLanguages() {
        String english = LanguageService.translate(ENGLISH, "Cifrar archivo");
        String basque = LanguageService.translate(BASQUE, english);

        assertEquals("Zifratu fitxategia", basque);
        assertEquals("Cifrar archivo", LanguageService.translate(SPANISH, basque));
        assertEquals(english, LanguageService.translate(ENGLISH, basque));
    }

    @Test
    void translatesNewViewsAndHelpCopyAcrossLanguages() {
        assertEquals("AES ENCRYPTION", LanguageService.translate(ENGLISH, "CIFRADO AES"));
        assertEquals("AES DESZIFRATZEA", LanguageService.translate(BASQUE, "DESCIFRADO AES"));
        assertEquals("← Back", LanguageService.translate(ENGLISH, "← Atrás"));
        assertEquals("Tokiko erabilera-gida",
                LanguageService.translate(BASQUE, "Manual de uso local"));
        assertEquals("Close", LanguageService.translate(ENGLISH, "Cerrar"));
        assertEquals("Keyboard shortcuts:",
                LanguageService.translate(ENGLISH, "Atajos de teclado:"));
        assertEquals("Ctrl+1: open the encryption view.",
                LanguageService.translate(ENGLISH, "Ctrl+1: abrir la vista de cifrado."));
        assertEquals("Ctrl+Enter: exekutatu uneko ikuspegiko eragiketa.",
                LanguageService.translate(BASQUE,
                        "Ctrl+Enter: ejecutar la operación de la vista actual."));

        String englishManual = LanguageService.translate(ENGLISH,
                "Cifrado: selecciona un archivo .txt, .log, .png o cualquier otro archivo binario y una carpeta "
                        + "de destino; pulsa Cifrar archivo y guarda la contraseña generada de 16 caracteres.\n\n"
                        + "Descifrado: selecciona el contenedor .enc, la carpeta de salida "
                        + "y la contraseña; pulsa Descifrar archivo. La comparación con el original es opcional.\n\n"
                        + "La vista previa se muestra después de un descifrado correcto para archivos de texto "
                        + "(.txt y .log) "
                        + "e imágenes PNG compatibles. Los archivos se procesan localmente y los resultados "
                        + "existentes no se sobrescriben.");
        assertTrue(englishManual.startsWith("Encryption: choose a .txt, .log, .png or any other binary file"));
        assertTrue(englishManual.contains("Files are processed locally"));
    }

    @Test
    void formatsProgressAndFilePathsWithoutTranslatingTheArguments() {
        String path = "C:\\usuarios\\Iñaki\\archivo original.txt";

        assertEquals("Encrypted file: " + path,
                LanguageService.format(ENGLISH, "Archivo cifrado: {0}", path));
        assertEquals("Zifratutako fitxategia: " + path,
                LanguageService.format(BASQUE, "Archivo cifrado: {0}", path));
        assertEquals("Encrypting file: 75%",
                LanguageService.format(ENGLISH, "Cifrando archivo: {0}%", 75));
        assertEquals("Authenticated preview. Full contents (42 bytes).",
                LanguageService.format(ENGLISH,
                        "Vista previa autenticada. Contenido completo ({0} bytes).", 42));
        assertEquals("Autentifikatutako aurrebista. Eduki osoa (42 byte).",
                LanguageService.format(BASQUE,
                        "Vista previa autenticada. Contenido completo ({0} bytes).", 42));
    }

    @Test
    void leavesUnknownTextAndNullValuesUnchanged() {
        String fileContent = "Do not translate this file content: ñ, € and 漢字";

        assertEquals(fileContent, LanguageService.translate(ENGLISH, fileContent));
        assertEquals(fileContent, LanguageService.translate(BASQUE, fileContent));
        assertNull(LanguageService.translate(ENGLISH, null));
    }
}
