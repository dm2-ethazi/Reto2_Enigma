package com.enigma.javafx.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServiceTest {
    private final CryptoService cryptoService = new CryptoService();

    @Test
    void generatesSixteenCharacterAlphanumericPasswords() {
        String password = cryptoService.generatePassword();

        assertEquals(CryptoService.PASSWORD_LENGTH, password.length());
        assertTrue(password.matches("[A-Za-z0-9]{16}"));
    }
}
