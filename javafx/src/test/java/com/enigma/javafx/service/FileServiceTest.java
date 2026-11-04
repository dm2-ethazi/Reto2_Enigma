package com.enigma.javafx.service;

import com.enigma.javafx.service.FileService.EncryptionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración del servicio de archivos y sus reglas de publicación.
 */
class FileServiceTest {
    /** Carpeta aislada que JUnit crea y elimina para cada prueba. */
    @TempDir
    Path temporaryDirectory;

    /** Servicio real usado para cifrar los archivos temporales. */
    private final FileService fileService = new FileService();

    /** Comprueba tamaños variados y que el cifrado no modifica los archivos de origen. */
    @Test
    void encryptsRequiredFileSizesWithoutChangingTheOriginal() throws Exception {
        int[] sizes = {0, 1, 1024 * 1024, 20 * 1024 * 1024};
        for (int size : sizes) {
            Path source = temporaryDirectory.resolve("input-" + size + ".bin");
            Path destination = Files.createDirectory(
                    temporaryDirectory.resolve("output-" + size));
            writePattern(source, size);
            byte[] originalHash = sha256(source);

            EncryptionResult result = fileService.encrypt(source, destination);

            assertEquals(source.getFileName() + ".enc", result.outputPath().getFileName().toString());
            assertEquals(size + CryptoService.HEADER_LENGTH + 16,
                    Files.size(result.outputPath()));
            assertArrayEquals(originalHash, sha256(source));
            assertTrue(result.password().matches("[A-Za-z0-9]{16}"));
        }
    }

    /** Confirma que un resultado existente no se sobrescribe ni se dejan temporales. */
    @Test
    void doesNotOverwriteAnExistingEncryptedFile() throws Exception {
        Path source = temporaryDirectory.resolve("input.txt");
        Path destination = Files.createDirectory(temporaryDirectory.resolve("output"));
        Files.writeString(source, "contenido original");
        EncryptionResult result = fileService.encrypt(source, destination);
        byte[] encryptedContent = Files.readAllBytes(result.outputPath());

        assertThrows(FileAlreadyExistsException.class,
                () -> fileService.encrypt(source, destination));
        assertArrayEquals(encryptedContent, Files.readAllBytes(result.outputPath()));
        try (Stream<Path> files = Files.list(destination)) {
            assertEquals(1, files.count());
        }
    }

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

    /** Valida también argumentos nulos y rutas que no son archivos normales o carpetas. */
    @Test
    void rejectsNullAndWrongTypePaths() throws Exception {
        Path destination = Files.createDirectory(temporaryDirectory.resolve("output-types"));
        Path source = Files.writeString(temporaryDirectory.resolve("valid-types.txt"), "texto");

        assertThrows(IOException.class, () -> fileService.encrypt(null, destination));
        assertThrows(IOException.class, () -> fileService.encrypt(source, null));
        assertThrows(IOException.class, () -> fileService.encrypt(temporaryDirectory, destination));
    }

    /** Elimina el archivo temporal si el procesamiento falla antes de publicar el resultado. */
    @Test
    void removesTemporaryOutputWhenProgressCallbackFails() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("interrupted.txt"), "contenido");
        Path destination = Files.createDirectory(temporaryDirectory.resolve("interrupted-output"));

        assertThrows(IllegalStateException.class, () -> fileService.encrypt(source, destination,
                ignored -> {
                    throw new IllegalStateException("Fallo de prueba");
                }));

        assertFalse(Files.exists(destination.resolve("interrupted.txt.enc")));
        try (Stream<Path> files = Files.list(destination)) {
            assertEquals(0, files.count());
        }
    }

    /** Escribe un patrón determinista para poder probar archivos grandes por bloques. */
    private static void writePattern(Path file, int size) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        try (var output = Files.newOutputStream(file)) {
            int offset = 0;
            while (offset < size) {
                int length = Math.min(buffer.length, size - offset);
                for (int i = 0; i < length; i++) {
                    int value = offset + i;
                    buffer[i] = (byte) (value * 31 + (value >>> 8));
                }
                output.write(buffer, 0, length);
                offset += length;
            }
        }
    }

    /** Calcula SHA-256 leyendo el archivo en bloques, sin cargarlo entero en memoria. */
    private static byte[] sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return digest.digest();
    }
}
