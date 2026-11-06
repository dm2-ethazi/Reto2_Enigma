package com.enigma.javafx.service;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServiceGeneralTest {
    /** Servicio criptogr?fico real sujeto a las pruebas. */
    private final CryptoService cryptoService = new CryptoService();

    /** Comprueba que las contraseñas tienen la longitud y el alfabeto contratados. */
    @Test
    void generatesSixteenCharacterAlphanumericPasswords() {
        String password = cryptoService.generatePassword();

        assertEquals(CryptoService.PASSWORD_LENGTH, password.length());
        assertTrue(password.matches("[A-Za-z0-9]{16}"));
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
}
