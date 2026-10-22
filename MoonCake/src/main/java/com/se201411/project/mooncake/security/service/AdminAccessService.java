package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {
    private final AccountRepository accounts;

    public AdminAccessService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public long requireAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof MoonCakeUserPrincipal principal)
                || principal.getRole() != AccountRole.ADMIN || !principal.isEnabled()) {
            throw new AccessDeniedException("Cần đăng nhập bằng tài khoản Admin đang hoạt động.");
        }
        var account = accounts.findByEmail(principal.getUsername()).orElseThrow(
                () -> new AccessDeniedException("Tài khoản không còn khả dụng."));
        if (!account.getId().equals(principal.getAccountId())
                || account.getRole() != AccountRole.ADMIN || account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản Admin đã bị vô hiệu hóa.");
        }
        return account.getId();
    }
}
