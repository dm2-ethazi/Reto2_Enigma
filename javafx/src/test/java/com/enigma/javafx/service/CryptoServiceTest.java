package com.enigma.javafx.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias del generador de contraseñas y del formato de cifrado R2AE.
 */
class CryptoServiceTest {
    /** Servicio real sujeto a las pruebas, sin sustitutos criptográficos. */
    private final CryptoService cryptoService = new CryptoService();

    /** Comprueba que las contraseñas tienen la longitud y el alfabeto contratados. */
    @Test
    void generatesSixteenCharacterAlphanumericPasswords() {
        String password = cryptoService.generatePassword();

        assertEquals(CryptoService.PASSWORD_LENGTH, password.length());
        assertTrue(password.matches("[A-Za-z0-9]{16}"));
    }

    /** Verifica la estructura de cabecera y el tamaño de varios tipos de contenido. */
    @Test
    void encryptsEmptyBinaryAndUtf8ContentIntoR2aeContainers() throws Exception {
        byte[][] originals = {
                new byte[0],
                new byte[] {0, 1, (byte) 0xff, 10, 0},
                "texto UTF-8: ñ y €".getBytes(StandardCharsets.UTF_8)
        };

        for (byte[] original : originals) {
            byte[] encrypted = encrypt(original);

            assertEquals(CryptoService.HEADER_LENGTH + original.length + 16, encrypted.length);
            assertArrayEquals("R2AE".getBytes(StandardCharsets.US_ASCII),
                    Arrays.copyOfRange(encrypted, 0, 4));
            assertEquals(1, Byte.toUnsignedInt(encrypted[4]));
            assertEquals(CryptoService.PBKDF2_ITERATIONS,
                    ByteBuffer.wrap(encrypted, 5, Integer.BYTES)
                            .order(ByteOrder.BIG_ENDIAN)
                            .getInt());
        }
    }

    /** Comprueba que cada cifrado obtiene valores nuevos para la sal y el nonce. */
    @Test
    void usesFreshSaltAndNonceForEveryEncryption() throws Exception {
        byte[] content = "same content".getBytes(StandardCharsets.UTF_8);
        byte[] first = encrypt(content);
        byte[] second = encrypt(content);

        assertTrue(!Arrays.equals(
                Arrays.copyOfRange(first, 9, CryptoService.HEADER_LENGTH),
                Arrays.copyOfRange(second, 9, CryptoService.HEADER_LENGTH)));
    }

    /** Verifica que el callback de progreso informa del total de bytes leídos. */
    @Test
    void reportsProcessedBytesWhileEncrypting() throws Exception {
        byte[] content = new byte[150_000];
        AtomicLong processedBytes = new AtomicLong();
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();

        cryptoService.encrypt(new ByteArrayInputStream(content), encrypted,
                "Password12345678".toCharArray(), processedBytes::set);

        assertEquals(content.length, processedBytes.get());
        assertEquals(CryptoService.HEADER_LENGTH + content.length + 16, encrypted.size());
    }

    /** Rechaza argumentos incompletos antes de iniciar operaciones criptográficas. */
    @Test
    void rejectsMissingStreamsCallbacksAndPasswords() {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(null, output, "Password12345678".toCharArray(), ignored -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(input, null, "Password12345678".toCharArray(), ignored -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(input, output, "Password12345678".toCharArray(), null));
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(input, output, new char[0]));
    }

    /** Cifra los bytes indicados en memoria y devuelve el contenedor producido. */
    private byte[] encrypt(byte[] content) throws Exception {
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        cryptoService.encrypt(new ByteArrayInputStream(content), encrypted,
                "Password12345678".toCharArray());
        return encrypted.toByteArray();
    }
}
