package com.enigma.javafx.service;

import com.enigma.javafx.service.FileService.EncryptionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileServiceGeneralTest {
    /** Servicio real usado para validar los flujos de archivos. */
    private final FileService fileService = new FileService();
    @TempDir
    Path temporaryDirectory;

    /** Comprueba el rechazo de rutas de entrada y destino inexistentes. */
    @Test
    void rejectsMissingSourceOrDestination() throws IOException {
        Path missingSource = temporaryDirectory.resolve("missing.txt");
        Path missingDirectory = temporaryDirectory.resolve("missing-output");

        assertThrows(IOException.class, () -> fileService.encrypt(missingSource, temporaryDirectory));
        Path validSource = temporaryDirectory.resolve("valid.txt");
        Files.writeString(validSource, "contenido");
        assertThrows(IOException.class, () -> fileService.encrypt(validSource, missingDirectory));
    }

    /** Rechaza destinos que son archivos y referencias inexistentes o que no son archivos. */
    @Test
    void rejectsInvalidDecryptionDestinationAndReferencePaths() throws Exception {
        Path source = Files.writeString(
                temporaryDirectory.resolve("path-validation.txt"), "texto de prueba");
        EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);
        Path outputFile = Files.writeString(
                temporaryDirectory.resolve("destination-is-file"), "no es una carpeta");
        Path missingDirectory = temporaryDirectory.resolve("missing-decryption-directory");

        assertThrows(IOException.class, () -> fileService.decrypt(
                encrypted.outputPath(), null, encrypted.password().toCharArray()));
        assertThrows(IOException.class, () -> fileService.decrypt(
                encrypted.outputPath(), missingDirectory, encrypted.password().toCharArray()));
        assertThrows(IOException.class, () -> fileService.decrypt(
                encrypted.outputPath(), outputFile, encrypted.password().toCharArray()));

        Path destination = Files.createDirectory(
                temporaryDirectory.resolve("reference-validation-output"));
        Path missingReference = temporaryDirectory.resolve("missing-reference.txt");
        assertThrows(IOException.class, () -> fileService.decrypt(
                encrypted.outputPath(), destination, encrypted.password().toCharArray(),
                missingReference, ignored -> { }));
        assertThrows(IOException.class, () -> fileService.decrypt(
                encrypted.outputPath(), destination, encrypted.password().toCharArray(),
                temporaryDirectory, ignored -> { }));
        assertDirectoryEmpty(destination);
    }

    /** Valida también argumentos nulos y rutas que no son archivos normales o carpetas. */
    @Test
    void rejectsNullAndWrongTypePaths() throws Exception {
        Path destination = Files.createDirectory(temporaryDirectory.resolve("output-types"));
        Path source = Files.writeString(temporaryDirectory.resolve("valid-types.txt"), "texto");

        assertThrows(IOException.class, () -> fileService.encrypt(null, destination));
        assertThrows(IOException.class, () -> fileService.encrypt(source, null));
        assertThrows(IOException.class, () -> fileService.encrypt(temporaryDirectory, destination));
    }

    private static void assertDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }
}
