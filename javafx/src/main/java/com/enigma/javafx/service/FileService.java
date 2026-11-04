package com.enigma.javafx.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.function.LongConsumer;

/**
 * Valida las rutas y coordina el cifrado seguro de ficheros, publicando el resultado
 * solo si la operación termina correctamente.
 */
public class FileService {
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
}
