package com.se201411.project.mooncake;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.PasswordResetToken;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.PasswordResetTokenRepository;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.PasswordResetService;
import com.se201411.project.mooncake.security.util.PasswordResetTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class PasswordResetIT {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void requestStoresOnlyHashedTokenForCustomer() {
        Account account = registerCustomer();

        passwordResetService.requestReset(account.getEmail().toUpperCase());

        assertThat(tokenRepository.findAll())
            .filteredOn(token -> token.getAccount().getId().equals(account.getId()))
            .singleElement()
            .satisfies(token -> {
                assertThat(token.getTokenHash()).hasSize(64).matches("[0-9a-f]{64}");
                assertThat(token.getUsedAt()).isNull();
                assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
            });
    }

    @Test
    void tokenChangesPasswordExactlyOnce() {
        Account account = registerCustomer();
        String rawToken = PasswordResetTokens.newRawToken();
        storeToken(account, rawToken, utcNow().plusMinutes(30));

        assertThat(passwordResetService.isTokenUsable(rawToken)).isTrue();
        passwordResetService.resetPassword(rawToken, "brandNew123");

        Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brandNew123", reloaded.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("password123", reloaded.getPasswordHash())).isFalse();

        assertThat(passwordResetService.isTokenUsable(rawToken)).isFalse();
        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "another123"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Account account = registerCustomer();
        String rawToken = PasswordResetTokens.newRawToken();
        storeToken(account, rawToken, utcNow().minusSeconds(1));

        assertThat(passwordResetService.isTokenUsable(rawToken)).isFalse();
        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "brandNew123"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    private Account registerCustomer() {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail(email);
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setFullName("Khách quên mật khẩu");
        form.setPhone("0901234567");
        authenticationService.registerCustomer(form);
        return accountRepository.findByEmail(email).orElseThrow();
    }

    private void storeToken(Account account, String rawToken, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setAccount(account);
        token.setTokenHash(PasswordResetTokens.hash(rawToken));
        token.setCreatedAt(expiresAt.minusMinutes(30));
        token.setExpiresAt(expiresAt);
        tokenRepository.saveAndFlush(token);
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
