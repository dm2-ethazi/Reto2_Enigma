package com.enigma.javafx.service;

import com.enigma.javafx.service.FileService.EncryptionResult;
import com.enigma.javafx.service.FileService.DecryptionResult;
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

class FileServiceDecryptionTest {
    /** Servicio real usado para validar los flujos de archivos. */
    private final FileService fileService = new FileService();
    @TempDir
    Path temporaryDirectory;

    /** Verifica el ciclo AES completo en todos los tamaños exigidos por el contrato. */
    @Test
    void roundTripsRequiredFileSizesWithExactBytesAndMatchingSha256() throws Exception {
        int[] sizes = {0, 1, 1024 * 1024, 20 * 1024 * 1024};
        Path encryptedDirectory = Files.createDirectory(
                temporaryDirectory.resolve("round-trip-encrypted"));
        Path decryptedDirectory = Files.createDirectory(
                temporaryDirectory.resolve("round-trip-decrypted"));

        for (int size : sizes) {
            Path source = temporaryDirectory.resolve("round-trip-" + size + ".bin");
            writePattern(source, size);
            byte[] originalHash = sha256(source);
            EncryptionResult encrypted = fileService.encrypt(source, encryptedDirectory);
            DecryptionResult decrypted = fileService.decrypt(encrypted.outputPath(),
                    decryptedDirectory, encrypted.password().toCharArray());

            assertEquals(size, Files.size(decrypted.outputPath()));
            assertEquals(-1, Files.mismatch(source, decrypted.outputPath()),
                    "Los bytes deben coincidir para el tamaño " + size);
            assertArrayEquals(originalHash, sha256(decrypted.outputPath()),
                    "SHA-256 debe coincidir para el tamaño " + size);
            assertArrayEquals(originalHash, sha256(source),
                    "El original debe permanecer intacto para el tamaño " + size);
        }
    }

    /** Descifra archivos, incluido uno vacío, y puede comprobarlos contra su referencia. */
    @Test
    void decryptsFilesAndVerifiesOptionalReference() throws Exception {
        byte[][] contents = {
                new byte[0],
                "contenido original ñ €".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        };
        Path destination = Files.createDirectory(temporaryDirectory.resolve("decrypted"));

        for (int index = 0; index < contents.length; index++) {
            Path source = temporaryDirectory.resolve("original-" + index + ".bin");
            Files.write(source, contents[index]);
            EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);

            DecryptionResult decrypted = fileService.decrypt(encrypted.outputPath(), destination,
                    encrypted.password().toCharArray(), source, ignored -> { });

            assertEquals(source.getFileName().toString(),
                    decrypted.outputPath().getFileName().toString());
            assertArrayEquals(contents[index], Files.readAllBytes(decrypted.outputPath()));
        }
    }

    /** No publica resultados parciales ante errores de autenticación o referencias distintas. */
    @Test
    void rejectsWrongPasswordTamperingAndReferenceMismatchWithoutPublishing() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("secure.txt"), "texto protegido");
        EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);
        Path destination = Files.createDirectory(temporaryDirectory.resolve("secure-output"));

        assertThrows(java.security.GeneralSecurityException.class,
                () -> fileService.decrypt(encrypted.outputPath(), destination,
                        "WrongPassword1234".toCharArray()));
        assertDirectoryEmpty(destination);

        Path incorrectReference = Files.writeString(
                temporaryDirectory.resolve("wrong-reference.txt"), "otro contenido");
        assertThrows(IOException.class, () -> fileService.decrypt(encrypted.outputPath(), destination,
                encrypted.password().toCharArray(), incorrectReference, ignored -> { }));
        assertDirectoryEmpty(destination);

        byte[] tamperedContent = Files.readAllBytes(encrypted.outputPath());
        tamperedContent[tamperedContent.length - 1] ^= 1;
        Files.write(encrypted.outputPath(), tamperedContent);
        assertThrows(java.security.GeneralSecurityException.class,
                () -> fileService.decrypt(encrypted.outputPath(), destination,
                        encrypted.password().toCharArray()));
        assertDirectoryEmpty(destination);
    }

    @Test
    void previewsOnlyTheRequestedPrefixAfterAuthenticatingTheWholeContainer() throws Exception {
        byte[] original = "contenido de vista previa autenticado".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        Path source = Files.write(temporaryDirectory.resolve("preview.txt"), original);
        Path destination = Files.createDirectory(
                temporaryDirectory.resolve("preview-destination"));
        EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);

        FileService.PreviewResult preview = fileService.preview(
                encrypted.outputPath(), encrypted.password().toCharArray(), 12);

        assertArrayEquals(java.util.Arrays.copyOf(original, 12), preview.content());
        assertEquals(original.length, preview.totalBytes());
        assertTrue(preview.truncated());
        assertFalse(Files.exists(destination.resolve("preview.txt")));
        assertThrows(IllegalArgumentException.class, () -> fileService.preview(
                encrypted.outputPath(), encrypted.password().toCharArray(), 0));
        assertThrows(IllegalArgumentException.class, () -> fileService.preview(
                encrypted.outputPath(), encrypted.password().toCharArray(),
                16 * 1024 * 1024 + 1));
    }

    @Test
    void rejectsWrongPasswordAndTamperingInsteadOfReturningUnauthenticatedPreview()
            throws Exception {
        Path source = Files.writeString(
                temporaryDirectory.resolve("preview-auth.txt"), "contenido protegido");
        EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);

        assertThrows(java.security.GeneralSecurityException.class, () -> fileService.preview(
                encrypted.outputPath(), "WrongPassword1234".toCharArray(), 1024));

        byte[] tampered = Files.readAllBytes(encrypted.outputPath());
        tampered[tampered.length - 1] ^= 1;
        Files.write(encrypted.outputPath(), tampered);
        assertThrows(java.security.GeneralSecurityException.class, () -> fileService.preview(
                encrypted.outputPath(), encrypted.password().toCharArray(), 1024));
    }

    /** Conserva el contenido de un destino preexistente y rechaza contenedores inválidos. */
    @Test
    void doesNotOverwriteDecryptedFileAndRejectsInvalidContainers() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("existing.txt"), "original");
        EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);
        Path destination = Files.createDirectory(temporaryDirectory.resolve("existing-output"));
        Path existingOutput = Files.writeString(destination.resolve("existing.txt"), "no sobrescribir");

        assertThrows(FileAlreadyExistsException.class,
                () -> fileService.decrypt(encrypted.outputPath(), destination,
                        encrypted.password().toCharArray()));
        assertEquals("no sobrescribir", Files.readString(existingOutput));

        Path invalidContainer = Files.writeString(temporaryDirectory.resolve("not-encrypted.txt"), "x");
        assertThrows(IOException.class, () -> fileService.decrypt(invalidContainer, destination,
                encrypted.password().toCharArray()));
        Path incompleteContainer = Files.write(temporaryDirectory.resolve("incomplete.enc"),
                new byte[CryptoService.HEADER_LENGTH + 15]);
        assertThrows(IOException.class, () -> fileService.decrypt(incompleteContainer, destination,
                encrypted.password().toCharArray()));
        assertThrows(IOException.class, () -> fileService.decrypt(encrypted.outputPath(), destination,
                new char[0]));
    }

    /** Elimina el temporal si falla el procesamiento durante el descifrado por bloques. */
    @Test
    void removesTemporaryOutputWhenDecryptionProgressCallbackFails() throws Exception {
        Path source = Files.writeString(
                temporaryDirectory.resolve("decrypt-interrupted.txt"), "contenido protegido");
        EncryptionResult encrypted = fileService.encrypt(source, temporaryDirectory);
        Path destination = Files.createDirectory(
                temporaryDirectory.resolve("decrypt-interrupted-output"));

        assertThrows(IllegalStateException.class, () -> fileService.decrypt(
                encrypted.outputPath(), destination, encrypted.password().toCharArray(),
                null, ignored -> {
                    throw new IllegalStateException("Fallo de prueba");
                }));

        assertDirectoryEmpty(destination);
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

    /** Comprueba que una operación fallida no deja ficheros temporales en el destino. */
    private static void assertDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.count());
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
