package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.AdminAccountForm;
import com.se201411.project.mooncake.security.dto.AdminAccountRow;
import com.se201411.project.mooncake.security.dto.AdminAccountsPageView;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.AdminAccountService;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminAccountServiceImpl implements AdminAccountService {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final AdminAccessService adminAccessService;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;
    private final Clock clock;

    public AdminAccountServiceImpl(
        AdminAccessService adminAccessService,
        AccountRepository accountRepository,
        PasswordEncoder passwordEncoder,
        SessionRegistry sessionRegistry,
        Clock clock
    ) {
        this.adminAccessService = adminAccessService;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAccountsPageView page() {
        long currentAdminId = adminAccessService.requireAdmin();
        long superAdminId = superAdminId();
        List<AdminAccountRow> rows = new ArrayList<>();
        for (Account account : accountRepository.findByRoleOrderByIdAsc(AccountRole.ADMIN)) {
            rows.add(AdminAccountRow.entityToDto(account, superAdminId, currentAdminId));
        }
        return new AdminAccountsPageView(rows, currentAdminId == superAdminId);
    }

    @Override
    @Transactional
    public void create(AdminAccountForm form) {
        requireSuperAdmin();
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        String password = form.getPassword();
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessRuleViolationException("Mật khẩu quá dài. Hãy dùng tối đa 64 ký tự không dấu.");
        }
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessRuleViolationException("Email này đã có tài khoản. Hãy dùng email khác.");
        }
        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(AccountRole.ADMIN);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now(clock));
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void lock(long accountId) {
        long superAdminId = requireSuperAdmin();
        if (accountId == superAdminId) {
            throw new BusinessRuleViolationException("Không thể khóa super admin.");
        }
        requireAdminAccount(accountId);
        int changed = accountRepository.updateStatus(
            accountId,
            AccountRole.ADMIN,
            AccountStatus.ACTIVE,
            AccountStatus.DISABLED
        );
        if (changed != 1) {
            throw new BusinessRuleViolationException("Tài khoản đã bị khóa trước đó.");
        }
        expireSessions(accountId);
    }

    @Override
    @Transactional
    public void unlock(long accountId) {
        requireSuperAdmin();
        requireAdminAccount(accountId);
        int changed = accountRepository.updateStatus(
            accountId,
            AccountRole.ADMIN,
            AccountStatus.DISABLED,
            AccountStatus.ACTIVE
        );
        if (changed != 1) {
            throw new BusinessRuleViolationException("Tài khoản đang hoạt động.");
        }
    }

    private long requireSuperAdmin() {
        long currentAdminId = adminAccessService.requireAdmin();
        long superAdminId = superAdminId();
        if (currentAdminId != superAdminId) {
            throw new BusinessRuleViolationException("Chỉ super admin được thêm hoặc khóa quản trị viên.");
        }
        return superAdminId;
    }

    private long superAdminId() {
        Account superAdmin = accountRepository.findFirstByRoleOrderByIdAsc(AccountRole.ADMIN)
            .orElseThrow(() -> new ResourceNotFoundException("Chưa có tài khoản quản trị."));
        return superAdmin.getId();
    }

    private void requireAdminAccount(long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên."));
        if (account.getRole() != AccountRole.ADMIN) {
            throw new ResourceNotFoundException("Không tìm thấy quản trị viên.");
        }
    }

    private void expireSessions(long accountId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!(principal instanceof MoonCakeUserPrincipal userPrincipal)) {
                continue;
            }
            if (userPrincipal.getAccountId() != accountId) {
                continue;
            }
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
            }
        }
    }
}
