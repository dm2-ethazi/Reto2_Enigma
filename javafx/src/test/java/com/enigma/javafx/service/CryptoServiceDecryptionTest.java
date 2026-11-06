package com.enigma.javafx.service;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoServiceDecryptionTest {
    /** Servicio criptogr?fico real sujeto a las pruebas. */
    private final CryptoService cryptoService = new CryptoService();

    /** Comprueba que se recuperan exactamente entradas binarias, UTF-8 y vacías. */
    @Test
    void decryptsEmptyBinaryAndUtf8ContentExactly() throws Exception {
        byte[][] originals = {
                new byte[0],
                new byte[] {0, 1, (byte) 0xff, 10, 0},
                "texto UTF-8: ñ y €".getBytes(StandardCharsets.UTF_8)
        };
        char[] password = "Password12345678".toCharArray();

        for (byte[] original : originals) {
            ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
            cryptoService.decrypt(new ByteArrayInputStream(encrypt(original)), decrypted, password);
            assertArrayEquals(original, decrypted.toByteArray());
        }
    }

    /** Rechaza contraseñas equivocadas, cabeceras alteradas y contenedores incompletos. */
    @Test
    void rejectsWrongPasswordAndTamperingInEveryHeaderField() throws Exception {
        byte[] encrypted = encrypt("contenido autenticado".getBytes(StandardCharsets.UTF_8));

        assertThrows(javax.crypto.AEADBadTagException.class,
                () -> cryptoService.decrypt(new ByteArrayInputStream(encrypted),
                        new ByteArrayOutputStream(),
                        "WrongPassword1234".toCharArray()));

        assertThrows(IOException.class,
                () -> cryptoService.decrypt(new ByteArrayInputStream(new byte[10]),
                        new ByteArrayOutputStream(), "Password12345678".toCharArray()));

        for (int headerByte = 0; headerByte < CryptoService.HEADER_LENGTH; headerByte++) {
            byte[] tamperedHeader = encrypted.clone();
            tamperedHeader[headerByte] ^= 1;
            Class<? extends Exception> expected = headerByte < 9
                    ? IOException.class : javax.crypto.AEADBadTagException.class;
            assertThrows(expected,
                    () -> cryptoService.decrypt(new ByteArrayInputStream(tamperedHeader),
                            new ByteArrayOutputStream(), "Password12345678".toCharArray()),
                    "Debe rechazarse el byte de cabecera " + headerByte);
        }

        byte[] tamperedContent = encrypted.clone();
        tamperedContent[tamperedContent.length - 1] ^= 1;
        assertThrows(javax.crypto.AEADBadTagException.class,
                () -> cryptoService.decrypt(new ByteArrayInputStream(tamperedContent),
                        new ByteArrayOutputStream(), "Password12345678".toCharArray()));
    }

    /** Informa cuántos bytes del contenedor se han procesado durante el descifrado. */
    @Test
    void reportsProcessedEncryptedBytesWhileDecrypting() throws Exception {
        byte[] original = new byte[150_000];
        byte[] encrypted = encrypt(original);
        AtomicLong processedBytes = new AtomicLong();

        cryptoService.decrypt(new ByteArrayInputStream(encrypted), new ByteArrayOutputStream(),
                "Password12345678".toCharArray(), processedBytes::set);

        assertEquals(encrypted.length - CryptoService.HEADER_LENGTH, processedBytes.get());
    }

    /** Cifra los bytes indicados en memoria y devuelve el contenedor producido. */
    private byte[] encrypt(byte[] content) throws Exception {
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        cryptoService.encrypt(new ByteArrayInputStream(content), encrypted,
                "Password12345678".toCharArray());
        return encrypted.toByteArray();
    }
}
