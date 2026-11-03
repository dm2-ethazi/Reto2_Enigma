package com.enigma.javafx.service;

import java.security.SecureRandom;
import java.util.Arrays;

public final class CryptoService {
    public static final int PASSWORD_LENGTH = 16;

    private static final char[] PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SecureRandom secureRandom;

    public CryptoService() {
        this(new SecureRandom());
    }

    CryptoService(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generatePassword() {
        char[] password = new char[PASSWORD_LENGTH];
        for (int i = 0; i < password.length; i++) {
            password[i] = PASSWORD_ALPHABET[secureRandom.nextInt(PASSWORD_ALPHABET.length)];
        }
        String generated = new String(password);
        Arrays.fill(password, '\0');
        return generated;
    }
}
