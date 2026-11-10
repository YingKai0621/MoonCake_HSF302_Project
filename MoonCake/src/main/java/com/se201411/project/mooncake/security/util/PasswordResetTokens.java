package com.se201411.project.mooncake.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Creates URL-safe reset tokens and the SHA-256 hash that is stored instead of the raw token.
 */
public final class PasswordResetTokens {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordResetTokens() {
    }

    public static String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /** Rejects obviously malformed input before it reaches the database. */
    public static boolean looksValid(String rawToken) {
        return rawToken != null && rawToken.matches("^[A-Za-z0-9_-]{43}$");
    }
}
