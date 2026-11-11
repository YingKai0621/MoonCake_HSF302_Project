package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.PasswordResetToken;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.repository.PasswordResetTokenRepository;
import com.se201411.project.mooncake.security.service.impl.PasswordResetServiceImpl;
import com.se201411.project.mooncake.security.util.PasswordResetTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-11-10T08:00:00Z"),
        ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 8, 0);

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetNotifier notifier;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void requestCreatesHashedTokenAndSendsLink() {
        Account account = account(AccountRole.CUSTOMER, AccountStatus.ACTIVE);
        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn A");
        when(accountRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(tokenRepository.existsByAccount_IdAndCreatedAtAfter(7L, NOW.minusSeconds(60))).thenReturn(false);
        when(customerRepository.findByAccount_Id(7L)).thenReturn(Optional.of(customer));

        service().requestReset("  Customer@Example.com ");

        verify(tokenRepository).invalidateOpenTokens(7L, NOW);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusMinutes(30));
        assertThat(saved.getUsedAt()).isNull();

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifier).sendResetLink(
            eq("customer@example.com"),
            eq("Nguyễn Văn A"),
            linkCaptor.capture(),
            eq(30L)
        );
        String link = linkCaptor.getValue();
        assertThat(link).startsWith("https://shop.example/reset-password?token=");
        String rawToken = link.substring(link.indexOf("token=") + "token=".length());
        assertThat(PasswordResetTokens.looksValid(rawToken)).isTrue();
        assertThat(saved.getTokenHash()).isEqualTo(PasswordResetTokens.hash(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void requestForUnknownEmailDoesNothing() {
        when(accountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service().requestReset("nobody@example.com");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(notifier);
    }

    @Test
    void requestForAdminOrDisabledAccountDoesNothing() {
        when(accountRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(account(AccountRole.ADMIN, AccountStatus.ACTIVE)));
        when(accountRepository.findByEmail("disabled@example.com"))
            .thenReturn(Optional.of(account(AccountRole.CUSTOMER, AccountStatus.DISABLED)));

        service().requestReset("admin@example.com");
        service().requestReset("disabled@example.com");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(notifier);
    }

    @Test
    void requestWithinResendWindowIsIgnored() {
        when(accountRepository.findByEmail("customer@example.com"))
            .thenReturn(Optional.of(account(AccountRole.CUSTOMER, AccountStatus.ACTIVE)));
        when(tokenRepository.existsByAccount_IdAndCreatedAtAfter(7L, NOW.minusSeconds(60))).thenReturn(true);

        service().requestReset("customer@example.com");

        verify(tokenRepository, never()).invalidateOpenTokens(anyLong(), any());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(notifier);
    }

    @Test
    void resetConsumesTokenAndStoresNewHash() {
        String rawToken = PasswordResetTokens.newRawToken();
        Account account = account(AccountRole.CUSTOMER, AccountStatus.ACTIVE);
        PasswordResetToken token = token(account, NOW.plusMinutes(10), null);
        when(tokenRepository.findWithAccountByTokenHash(PasswordResetTokens.hash(rawToken)))
            .thenReturn(Optional.of(token));
        when(tokenRepository.consume(99L, NOW)).thenReturn(1);
        when(passwordEncoder.encode("newPassword1")).thenReturn("encoded-new");

        service().resetPassword(rawToken, "newPassword1");

        assertThat(account.getPasswordHash()).isEqualTo("encoded-new");
        verify(accountRepository).save(account);
    }

    @Test
    void resetRejectsExpiredToken() {
        String rawToken = PasswordResetTokens.newRawToken();
        PasswordResetToken token = token(
            account(AccountRole.CUSTOMER, AccountStatus.ACTIVE),
            NOW.minusSeconds(1),
            null
        );
        when(tokenRepository.findWithAccountByTokenHash(PasswordResetTokens.hash(rawToken)))
            .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service().resetPassword(rawToken, "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(tokenRepository, never()).consume(anyLong(), any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resetRejectsAlreadyUsedToken() {
        String rawToken = PasswordResetTokens.newRawToken();
        PasswordResetToken token = token(
            account(AccountRole.CUSTOMER, AccountStatus.ACTIVE),
            NOW.plusMinutes(10),
            NOW.minusMinutes(1)
        );
        when(tokenRepository.findWithAccountByTokenHash(PasswordResetTokens.hash(rawToken)))
            .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service().resetPassword(rawToken, "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resetLosesRaceWhenConditionalConsumeFails() {
        String rawToken = PasswordResetTokens.newRawToken();
        PasswordResetToken token = token(
            account(AccountRole.CUSTOMER, AccountStatus.ACTIVE),
            NOW.plusMinutes(10),
            null
        );
        when(tokenRepository.findWithAccountByTokenHash(PasswordResetTokens.hash(rawToken)))
            .thenReturn(Optional.of(token));
        when(tokenRepository.consume(99L, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service().resetPassword(rawToken, "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(passwordEncoder, never()).encode(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void malformedTokenIsRejectedWithoutDatabaseLookup() {
        assertThat(service().isTokenUsable("not-a-token")).isFalse();
        assertThat(service().isTokenUsable(null)).isFalse();
        assertThatThrownBy(() -> service().resetPassword("bad", "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void usableTokenIsReported() {
        String rawToken = PasswordResetTokens.newRawToken();
        when(tokenRepository.findWithAccountByTokenHash(PasswordResetTokens.hash(rawToken)))
            .thenReturn(Optional.of(token(
                account(AccountRole.CUSTOMER, AccountStatus.ACTIVE),
                NOW.plusMinutes(5),
                null
            )));

        assertThat(service().isTokenUsable(rawToken)).isTrue();
    }

    private PasswordResetServiceImpl service() {
        return new PasswordResetServiceImpl(
            accountRepository,
            customerRepository,
            tokenRepository,
            notifier,
            passwordEncoder,
            FIXED_CLOCK,
            "https://shop.example/",
            30,
            60
        );
    }

    private static Account account(AccountRole role, AccountStatus status) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 7L);
        account.setEmail("customer@example.com");
        account.setPasswordHash("old-hash");
        account.setRole(role);
        account.setStatus(status);
        account.setCreatedAt(NOW.minusDays(1));
        return account;
    }

    private static PasswordResetToken token(Account account, LocalDateTime expiresAt, LocalDateTime usedAt) {
        PasswordResetToken token = new PasswordResetToken();
        ReflectionTestUtils.setField(token, "id", 99L);
        token.setAccount(account);
        token.setCreatedAt(NOW.minusMinutes(20));
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
