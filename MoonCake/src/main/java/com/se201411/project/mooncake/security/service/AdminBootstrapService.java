package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.security.repository.AdminBootstrapRepository;
import jakarta.validation.constraints.*;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Locale;

@Service
@Profile("bootstrap-admin")
public class AdminBootstrapService {
    private final AdminBootstrapRepository repo;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final Clock clock;

    public AdminBootstrapService(AdminBootstrapRepository repo, PasswordEncoder encoder, Validator validator, Clock clock) {
        this.repo = repo;
        this.encoder = encoder;
        this.validator = validator;
        this.clock = clock;
    }

    public record Credentials(@NotBlank @Email @Size(max = 254) String email,
                              @NotNull @Size(min = 8, max = 64) String password) {
    }

    @Transactional
    public boolean createIfAbsent(String email, String password) {
        if (repo.adminExistsUnderLock()) return false;
        String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        if (!validator.validate(new Credentials(normalized, password)).isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Cần BOOTSTRAP_ADMIN_EMAIL hợp lệ và BOOTSTRAP_ADMIN_PASSWORD 8–64 ký tự, tối đa 72 byte UTF-8.");
        if (repo.emailExists(normalized))
            throw new IllegalStateException("Email đã được sử dụng; không thay đổi quyền tài khoản hiện có.");
        Account account = new Account();
        account.setEmail(normalized);
        account.setPasswordHash(encoder.encode(password));
        account.setRole(AccountRole.ADMIN);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now(clock));
        repo.save(account);
        return true;
    }
}
