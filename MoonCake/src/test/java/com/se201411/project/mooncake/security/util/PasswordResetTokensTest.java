package com.se201411.project.mooncake.security.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokensTest {

    @Test
    void generatesDistinctUrlSafeTokens() {
        String first = PasswordResetTokens.newRawToken();
        String second = PasswordResetTokens.newRawToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(PasswordResetTokens.looksValid(first)).isTrue();
        assertThat(PasswordResetTokens.looksValid(second)).isTrue();
    }

    @Test
    void hashesToStableLowercaseHex() {
        assertThat(PasswordResetTokens.hash("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void rejectsMalformedTokens() {
        assertThat(PasswordResetTokens.looksValid(null)).isFalse();
        assertThat(PasswordResetTokens.looksValid("")).isFalse();
        assertThat(PasswordResetTokens.looksValid("short")).isFalse();
        assertThat(PasswordResetTokens.looksValid("a".repeat(42) + "=")).isFalse();
    }
}
