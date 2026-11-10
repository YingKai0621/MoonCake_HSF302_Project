package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.PasswordResetToken;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.repository.PasswordResetTokenRepository;
import com.se201411.project.mooncake.security.service.PasswordResetNotifier;
import com.se201411.project.mooncake.security.service.PasswordResetService;
import com.se201411.project.mooncake.security.util.EmailNormalizer;
import com.se201411.project.mooncake.security.util.PasswordResetTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetNotifier notifier;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String publicUrl;
    private final long tokenMinutes;
    private final long resendSeconds;

    public PasswordResetServiceImpl(
        AccountRepository accountRepository,
        CustomerRepository customerRepository,
        PasswordResetTokenRepository tokenRepository,
        PasswordResetNotifier notifier,
        PasswordEncoder passwordEncoder,
        Clock clock,
        @Value("${mooncake.public-url:http://localhost:8080}") String publicUrl,
        @Value("${mooncake.password-reset.token-minutes:30}") long tokenMinutes,
        @Value("${mooncake.password-reset.resend-seconds:60}") long resendSeconds
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.tokenRepository = tokenRepository;
        this.notifier = notifier;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.publicUrl = stripTrailingSlash(publicUrl);
        this.tokenMinutes = tokenMinutes;
        this.resendSeconds = resendSeconds;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            return;
        }

        Optional<Account> found = accountRepository.findByEmail(normalizedEmail)
            .filter(PasswordResetServiceImpl::isActiveCustomer);
        if (found.isEmpty()) {
            return;
        }
        Account account = found.get();
        LocalDateTime now = LocalDateTime.now(clock);

        if (tokenRepository.existsByAccount_IdAndCreatedAtAfter(
            account.getId(),
            now.minusSeconds(resendSeconds)
        )) {
            return;
        }

        tokenRepository.invalidateOpenTokens(account.getId(), now);

        String rawToken = PasswordResetTokens.newRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setAccount(account);
        token.setTokenHash(PasswordResetTokens.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(tokenMinutes));
        tokenRepository.save(token);

        String recipientName = customerRepository.findByAccount_Id(account.getId())
            .map(Customer::getFullName)
            .orElse(null);
        String resetLink = publicUrl + "/reset-password?token=" + rawToken;

        afterCommit(() -> notifier.sendResetLink(
            account.getEmail(),
            recipientName,
            resetLink,
            tokenMinutes
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenUsable(String rawToken) {
        return findUsableToken(rawToken, LocalDateTime.now(clock)).isPresent();
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = findUsableToken(rawToken, now)
            .orElseThrow(InvalidPasswordResetTokenException::new);

        Account account = token.getAccount();
        if (tokenRepository.consume(token.getId(), now) != 1) {
            throw new InvalidPasswordResetTokenException();
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    private Optional<PasswordResetToken> findUsableToken(String rawToken, LocalDateTime now) {
        if (!PasswordResetTokens.looksValid(rawToken)) {
            return Optional.empty();
        }
        return tokenRepository.findWithAccountByTokenHash(PasswordResetTokens.hash(rawToken))
            .filter(token -> token.getUsedAt() == null)
            .filter(token -> token.getExpiresAt().isAfter(now))
            .filter(token -> isActiveCustomer(token.getAccount()));
    }

    private static boolean isActiveCustomer(Account account) {
        return account.getRole() == AccountRole.CUSTOMER
            && account.getStatus() == AccountStatus.ACTIVE;
    }

    /** Sends mail only once the token row is committed; runs immediately outside a transaction. */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url == null ? "" : url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
