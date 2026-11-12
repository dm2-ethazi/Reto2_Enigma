package com.enigma.javafx.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Valida las rutas y coordina el cifrado seguro de ficheros, publicando el resultado
 * solo si la operación termina correctamente.
 */
public class FileService {
    private static final int MAX_PREVIEW_BUFFER_BYTES = 16 * 1024 * 1024;
    /** Servicio criptográfico usado para crear la contraseña y cifrar el contenido. */
    private final CryptoService cryptoService;

    /** Crea el servicio de archivos con un servicio criptográfico predeterminado. */
    public FileService() {
        this(new CryptoService());
    }

    /** Crea el servicio con una dependencia criptográfica inyectada para pruebas. */
    FileService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    /**
     * Cifra un archivo y devuelve la ubicación del contenedor y la contraseña generada.
     *
     * @param source fichero original
     * @param destinationDirectory carpeta donde se guardará el contenedor
     * @return ruta del contenedor cifrado y contraseña que permite recuperarlo
     * @throws IOException si las rutas o las operaciones de archivos no son válidas
     * @throws GeneralSecurityException si falla el servicio criptográfico
     */
    public EncryptionResult encrypt(Path source, Path destinationDirectory)
            throws IOException, GeneralSecurityException {
        return encrypt(source, destinationDirectory, ignored -> { });
    }

    /**
     * Cifra un archivo en bloques, conservando el origen y evitando sobrescribir resultados.
     *
     * @param source archivo original que se va a cifrar
     * @param destinationDirectory carpeta donde se publicará el archivo .enc
     * @param processedBytes callback con el total de bytes originales procesados
     * @return archivo cifrado y contraseña que debe guardarse por separado
     * @throws IOException si no se pueden validar, leer o publicar los archivos
     * @throws GeneralSecurityException si no se puede cifrar el contenido
     */
    public EncryptionResult encrypt(Path source, Path destinationDirectory,
                                    LongConsumer processedBytes)
            throws IOException, GeneralSecurityException {
        Path inputFile = validateSource(source);
        Path outputDirectory = validateDestinationDirectory(destinationDirectory);
        Path outputFile = outputDirectory.resolve(inputFile.getFileName() + ".enc");
        if (Files.exists(outputFile)) {
            throw new FileAlreadyExistsException(outputFile.toString(), null,
                    "El archivo de destino ya existe; no se sobrescribirá.");
        }

        // El temporal evita dejar un .enc parcial visible si falla la operación.
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
            // Solo se publica cuando el flujo se ha cifrado y cerrado correctamente.
            Files.move(temporaryFile, outputFile);
            published = true;
            return new EncryptionResult(outputFile, password);
        } finally {
            // Limpia la copia mutable y elimina restos si no se llegó a publicar el resultado.
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '\0');
            }
            if (!published) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    /**
     * Descifra un contenedor R2AE a la carpeta indicada sin sobrescribir archivos existentes.
     *
     * @param source contenedor cifrado con extensión .enc
     * @param destinationDirectory carpeta donde se publicará el archivo recuperado
     * @param password contraseña usada para cifrar el contenedor
     * @return ruta del archivo descifrado
     * @throws IOException si las rutas no son válidas, el contenedor falla o no se puede publicar
     * @throws GeneralSecurityException si la contraseña no es correcta o falla la autenticación
     */
    public DecryptionResult decrypt(Path source, Path destinationDirectory, char[] password)
            throws IOException, GeneralSecurityException {
        return decrypt(source, destinationDirectory, password, null, ignored -> { });
    }

    /**
     * Descifra un contenedor por bloques y publica el resultado solo después de validar GCM.
     * Si se proporciona un archivo de referencia, compara sus bytes con el resultado antes
     * de mover el temporal a su destino definitivo.
     *
     * @param source contenedor cifrado con extensión .enc
     * @param destinationDirectory carpeta donde se publicará el archivo recuperado
     * @param password contraseña usada para cifrar el contenedor
     * @param referenceFile archivo original opcional para comprobar igualdad exacta
     * @param processedBytes callback con el total de bytes cifrados procesados
     * @return ruta del archivo descifrado
     * @throws IOException si las rutas no son válidas, no coincide la referencia o falla el I/O
     * @throws GeneralSecurityException si la contraseña no es correcta o falla la autenticación
     */
    public DecryptionResult decrypt(Path source, Path destinationDirectory, char[] password,
                                    Path referenceFile, LongConsumer processedBytes)
            throws IOException, GeneralSecurityException {
        Path encryptedFile = validateSource(source);
        Path outputDirectory = validateDestinationDirectory(destinationDirectory);
        if (password == null || password.length == 0) {
            throw new IOException("Introduce la contraseña del contenedor.");
        }
        validateEncryptedContainer(encryptedFile);

        Path outputFile = outputDirectory.resolve(decryptedFileName(encryptedFile));
        if (Files.exists(outputFile)) {
            throw new FileAlreadyExistsException(outputFile.toString(), null,
                    "El archivo de destino ya existe; no se sobrescribirá.");
        }

        Path normalizedReference = referenceFile == null ? null : validateSource(referenceFile);
        Path temporaryFile = Files.createTempFile(outputDirectory, ".r2ae-", ".part");
        boolean published = false;
        try {
            try (InputStream input = Files.newInputStream(encryptedFile);
                 OutputStream output = Files.newOutputStream(temporaryFile)) {
                cryptoService.decrypt(input, output, password, processedBytes);
            }
            if (normalizedReference != null && Files.mismatch(temporaryFile, normalizedReference) != -1) {
                throw new IOException("El archivo descifrado no coincide con el original de referencia.");
            }
            Files.move(temporaryFile, outputFile);
            published = true;
            return new DecryptionResult(outputFile);
        } finally {
            if (!published) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    /**
     * Descifra un contenedor para previsualizar una cantidad limitada de bytes.
     * El flujo completo se procesa para validar la autenticidad GCM, pero solo
     * se conserva en memoria el prefijo indicado.
     *
     * @param source contenedor cifrado
     * @param password contraseña usada para cifrar el contenedor
     * @param maximumBytes máximo de bytes que se conservarán para la vista previa
     * @return bytes conservados, tamaño descifrado total e indicador de truncamiento
     * @throws IOException si el contenedor no es válido o no se puede leer
     * @throws GeneralSecurityException si la contraseña o la autenticidad no son válidas
     * @throws IllegalArgumentException si el límite o el callback no son válidos
     */
    public PreviewResult preview(Path source, char[] password, int maximumBytes)
            throws IOException, GeneralSecurityException {
        return preview(source, password, maximumBytes, ignored -> { });
    }

    /**
     * Descifra y autentica completamente un contenedor sin escribir el archivo resultante.
     *
     * @param source contenedor cifrado
     * @param password contraseña usada para cifrar el contenedor
     * @param maximumBytes máximo de bytes que se conservarán para la vista previa
     * @param processedBytes callback con el total de bytes cifrados procesados
     * @return bytes conservados, tamaño descifrado total e indicador de truncamiento
     * @throws IOException si el contenedor no es válido o no se puede leer
     * @throws GeneralSecurityException si la contraseña o la autenticidad no son válidas
     * @throws IllegalArgumentException si el límite o el callback no son válidos
     */
    public PreviewResult preview(Path source, char[] password, int maximumBytes,
                                 LongConsumer processedBytes)
            throws IOException, GeneralSecurityException {
        Path encryptedFile = validateSource(source);
        if (password == null || password.length == 0) {
            throw new IOException("Introduce la contraseña del contenedor.");
        }
        if (maximumBytes <= 0 || maximumBytes > MAX_PREVIEW_BUFFER_BYTES) {
            throw new IllegalArgumentException(
                    "El límite de la vista previa debe estar entre 1 y 16777216 bytes.");
        }
        if (processedBytes == null) {
            throw new IllegalArgumentException("El callback de progreso es obligatorio.");
        }
        validateEncryptedContainer(encryptedFile);

        PreviewOutputStream previewOutput = new PreviewOutputStream(maximumBytes);
        try (InputStream input = Files.newInputStream(encryptedFile)) {
            cryptoService.decrypt(input, previewOutput, password, processedBytes);
            return previewOutput.toResult();
        } finally {
            previewOutput.clear();
        }
    }

    private static void validateEncryptedContainer(Path encryptedFile) throws IOException {
        String fileName = encryptedFile.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!fileName.endsWith(".enc")) {
            throw new IOException("Selecciona un contenedor cifrado con extensión .enc.");
        }
        if (Files.size(encryptedFile) < CryptoService.HEADER_LENGTH + 16L) {
            throw new IOException("El contenedor R2AE está incompleto.");
        }
    }

    /** Retira el sufijo .enc para recuperar el nombre original del archivo. */
    private static String decryptedFileName(Path encryptedFile) {
        String fileName = encryptedFile.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".enc".length());
    }

    /**
     * Comprueba que el origen sea un archivo normal existente y legible, y normaliza su ruta.
     *
     * @param source ruta introducida para el archivo de origen
     * @return ruta absoluta normalizada del archivo
     * @throws IOException si la ruta no existe, no es un archivo o no se puede leer
     */
    private static Path validateSource(Path source) throws IOException {
        if (source == null) {
            throw new IOException("Selecciona un fichero de origen.");
        }
        Path normalized = source.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || !Files.isReadable(normalized)) {
            throw new IOException("El fichero de origen no existe o no se puede leer.");
        }
        return normalized;
    }

    /**
     * Comprueba que el destino sea una carpeta existente y escribible, y normaliza su ruta.
     *
     * @param destinationDirectory ruta introducida para la carpeta de salida
     * @return ruta absoluta normalizada de la carpeta
     * @throws IOException si la ruta no existe, no es una carpeta o no permite escribir
     */
    private static Path validateDestinationDirectory(Path destinationDirectory) throws IOException {
        if (destinationDirectory == null) {
            throw new IOException("Selecciona una carpeta de destino.");
        }
        Path normalized = destinationDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized) || !Files.isWritable(normalized)) {
            throw new IOException("La carpeta de destino no existe o no permite escribir.");
        }
        return normalized;
    }

    /**
     * Resultado del cifrado que la interfaz necesita para mostrar y entregar la credencial.
     *
     * @param outputPath ruta del contenedor publicado
     * @param password contraseña generada para cifrar ese contenedor
     */
    public record EncryptionResult(Path outputPath, String password) {
    }

    /** Ruta del archivo publicado tras validar correctamente la autenticidad del contenedor. */
    public record DecryptionResult(Path outputPath) {
    }

    /** Prefijo autenticado disponible para la vista previa de un archivo. */
    public record PreviewResult(byte[] content, long totalBytes, boolean truncated) {
    }

    private static final class PreviewOutputStream extends OutputStream {
        private final byte[] previewBuffer;
        private int storedBytes;
        private long totalBytes;

        private PreviewOutputStream(int maximumBytes) {
            previewBuffer = new byte[maximumBytes];
        }

        @Override
        public void write(int value) {
            if (storedBytes < previewBuffer.length) {
                previewBuffer[storedBytes++] = (byte) value;
            }
            totalBytes++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            int bytesToStore = Math.min(length, previewBuffer.length - storedBytes);
            if (bytesToStore > 0) {
                System.arraycopy(bytes, offset, previewBuffer, storedBytes, bytesToStore);
                storedBytes += bytesToStore;
            }
            totalBytes += length;
        }

        private PreviewResult toResult() {
            return new PreviewResult(
                    Arrays.copyOf(previewBuffer, storedBytes),
                    totalBytes,
                    totalBytes > previewBuffer.length);
        }

        private void clear() {
            Arrays.fill(previewBuffer, (byte) 0);
            storedBytes = 0;
            totalBytes = 0;
        }
    }
}
