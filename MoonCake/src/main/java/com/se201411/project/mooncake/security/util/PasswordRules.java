package com.se201411.project.mooncake.security.util;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {

    /** BCrypt only reads the first 72 bytes of the UTF-8 password. */
    public static final int BCRYPT_MAX_BYTES = 72;

    private PasswordRules() {
    }

    public static boolean exceedsBcryptLimit(String password) {
        return password != null
            && password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
