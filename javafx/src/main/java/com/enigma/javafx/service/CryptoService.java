package com.enigma.javafx.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.LongConsumer;

/**
 * Crea contraseñas y cifra flujos de bytes con AES-256-GCM en el formato R2AE v1.
 * La clave se deriva mediante PBKDF2-HMAC-SHA256; la cabecera se autentica junto
 * con el contenido y no contiene la contraseña.
 */
public final class CryptoService {
    /** Número de caracteres de cada contraseña generada. */
    public static final int PASSWORD_LENGTH = 16;
    /** Iteraciones PBKDF2-HMAC-SHA256 usadas para derivar la clave AES. */
    public static final int PBKDF2_ITERATIONS = 200_000;
    /** Tamaño de la cabecera R2AE en bytes. */
    public static final int HEADER_LENGTH = 37;

    /** Tamaño aleatorio de la sal usada para derivar la clave. */
    private static final int SALT_LENGTH = 16;
    /** Tamaño del nonce único de GCM para cada cifrado. */
    private static final int NONCE_LENGTH = 12;
    /** Tamaño de la etiqueta de autenticación GCM, en bits. */
    private static final int TAG_LENGTH_BITS = 128;
    /** Longitud de la clave AES derivada, en bits. */
    private static final int AES_KEY_LENGTH_BITS = 256;
    /** Tamaño de los bloques usados para cifrar sin cargar el archivo entero en memoria. */
    private static final int BUFFER_SIZE = 64 * 1024;
    /** Versión actual del formato de contenedor R2AE. */
    private static final byte FORMAT_VERSION = 1;
    /** Identificador ASCII del formato R2AE. */
    private static final byte[] MAGIC = "R2AE".getBytes(StandardCharsets.US_ASCII);
    /** Alfabeto permitido para las contraseñas automáticas. */
    private static final char[] PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    /** Fuente criptográficamente segura para contraseñas, sal y nonce. */
    private final SecureRandom secureRandom;

    /** Crea el servicio usando el generador criptográfico seguro predeterminado de Java. */
    public CryptoService() {
        this(new SecureRandom());
    }

    /** Crea el servicio con una fuente aleatoria inyectada, útil para pruebas controladas. */
    CryptoService(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /**
     * Genera una contraseña alfanumérica con selección aleatoria criptográficamente segura.
     * El arreglo temporal se limpia después de crear el texto que se devuelve.
     *
     * @return contraseña nueva de {@value #PASSWORD_LENGTH} caracteres
     */
    public String generatePassword() {
        char[] password = new char[PASSWORD_LENGTH];
        for (int i = 0; i < password.length; i++) {
            password[i] = PASSWORD_ALPHABET[secureRandom.nextInt(PASSWORD_ALPHABET.length)];
        }
        try {
            return new String(password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /** Cifra un flujo completo sin notificar el progreso de lectura. */
    public void encrypt(InputStream input, OutputStream output, char[] password)
            throws IOException, GeneralSecurityException {
        encrypt(input, output, password, ignored -> { });
    }

    /**
     * Cifra un flujo con AES-256-GCM y emite una cabecera R2AE autenticada.
     * La cabecera contiene la firma del formato, versión, iteraciones PBKDF2,
     * sal y nonce, en ese orden y con enteros en orden big-endian.
     *
     * @param input bytes originales que se van a cifrar
     * @param output destino donde se escribe la cabecera y el contenido cifrado
     * @param password contraseña usada para derivar la clave AES
     * @param processedBytes callback que recibe el total de bytes originales procesados
     * @throws IllegalArgumentException si falta un flujo, callback o contraseña
     * @throws IOException si ocurre un error al leer o escribir los flujos
     * @throws GeneralSecurityException si la plataforma no puede configurar la criptografía
     */
    public void encrypt(InputStream input, OutputStream output, char[] password,
                        LongConsumer processedBytes)
            throws IOException, GeneralSecurityException {
        if (input == null || output == null || processedBytes == null) {
            throw new IllegalArgumentException("Los flujos y el callback de progreso son obligatorios.");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(nonce);

        byte[] header = ByteBuffer.allocate(HEADER_LENGTH)
                .order(ByteOrder.BIG_ENDIAN)
                .put(MAGIC)
                .put(FORMAT_VERSION)
                .putInt(PBKDF2_ITERATIONS)
                .put(salt)
                .put(nonce)
                .array();

        byte[] key = deriveKey(password, salt);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            // Vincula también los metadatos: cualquier alteración de la cabecera invalida el contenido.
            cipher.updateAAD(header);
            output.write(header);
            encryptStream(input, output, cipher, processedBytes);
        } finally {
            // Reduce el tiempo durante el que la clave derivada permanece en memoria.
            Arrays.fill(key, (byte) 0);
        }
    }

    /**
     * Deriva una clave AES de 256 bits a partir de la contraseña y la sal.
     * PBEKeySpec mantiene una copia interna y por eso se limpia al terminar.
     *
     * @param password contraseña fuente para PBKDF2
     * @param salt sal aleatoria específica de este cifrado
     * @return bytes de la clave AES derivada
     * @throws GeneralSecurityException si el proveedor no puede derivar la clave
     */
    private static byte[] deriveKey(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
        } finally {
            keySpec.clearPassword();
        }
    }

    /**
     * Cifra el flujo por bloques para mantener acotado el uso de memoria.
     * La llamada final a {@code doFinal} también escribe la etiqueta de autenticación GCM.
     *
     * @param input flujo con los bytes originales
     * @param output destino del contenido cifrado
     * @param cipher instancia GCM ya inicializada y con la cabecera autenticada
     * @param processedBytes callback para informar el total acumulado de bytes leídos
     * @throws IOException si falla la lectura o escritura
     * @throws GeneralSecurityException si falla el cierre criptográfico de GCM
     */
    private static void encryptStream(InputStream input, OutputStream output, Cipher cipher,
                                     LongConsumer processedBytes)
            throws IOException, GeneralSecurityException {
        byte[] inputBuffer = new byte[BUFFER_SIZE];
        long processed = 0;
        int read;
        while ((read = input.read(inputBuffer)) != -1) {
            if (read == 0) {
                // Evita un ciclo infinito con flujos que temporalmente informan cero bytes leídos.
                int value = input.read();
                if (value == -1) {
                    break;
                }
                inputBuffer[0] = (byte) value;
                read = 1;
            }
            byte[] encrypted = cipher.update(inputBuffer, 0, read);
            if (encrypted != null && encrypted.length > 0) {
                output.write(encrypted);
            }
            processed += read;
            processedBytes.accept(processed);
        }

        // GCM emite aquí los últimos bytes pendientes y su etiqueta de autenticación.
        byte[] finalBlock = cipher.doFinal();
        if (finalBlock != null && finalBlock.length > 0) {
            output.write(finalBlock);
        }
    }
}
