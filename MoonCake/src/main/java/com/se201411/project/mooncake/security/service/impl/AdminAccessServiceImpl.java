package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessServiceImpl implements AdminAccessService {

    private final AccountRepository accounts;

    public AdminAccessServiceImpl(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public long requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Cần đăng nhập bằng tài khoản Admin đang hoạt động.");
        }
        if (!(authentication.getPrincipal() instanceof MoonCakeUserPrincipal principal)) {
            throw new AccessDeniedException("Cần đăng nhập bằng tài khoản Admin đang hoạt động.");
        }
        if (principal.getRole() != AccountRole.ADMIN || !principal.isEnabled()) {
            throw new AccessDeniedException("Cần đăng nhập bằng tài khoản Admin đang hoạt động.");
        }
        Account account = accounts.findByEmail(principal.getUsername())
            .orElseThrow(() -> new AccessDeniedException("Tài khoản không còn khả dụng."));
        boolean sameAccount = account.getId().equals(principal.getAccountId());
        boolean activeAdmin = account.getRole() == AccountRole.ADMIN && account.getStatus() == AccountStatus.ACTIVE;
        if (!sameAccount || !activeAdmin) {
            throw new AccessDeniedException("Tài khoản Admin đã bị vô hiệu hóa.");
        }
        return account.getId();
    }
}
