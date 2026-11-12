package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.dto.AdminAccountForm;
import com.se201411.project.mooncake.security.dto.AdminAccountsPageView;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.service.impl.AdminAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAccountServiceImplTest {
    private final AdminAccessService access = mock(AdminAccessService.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SessionRegistry sessions = mock(SessionRegistry.class);
    private final AdminAccountServiceImpl service = new AdminAccountServiceImpl(
        access, accounts, encoder, sessions, Clock.systemUTC()
    );
    private final Account superAdmin = admin(1L, AccountStatus.ACTIVE);
    private final Account staff = admin(2L, AccountStatus.ACTIVE);

    @BeforeEach
    void setUp() {
        when(accounts.findFirstByRoleOrderByIdAsc(AccountRole.ADMIN)).thenReturn(Optional.of(superAdmin));
        when(accounts.findById(2L)).thenReturn(Optional.of(staff));
        when(accounts.findByRoleOrderByIdAsc(AccountRole.ADMIN)).thenReturn(List.of(superAdmin, staff));
        when(encoder.encode("matkhau123")).thenReturn("hashed");
    }

    @Test
    void superAdminCreatesAnActiveAdminWithAHashedPassword() {
        when(access.requireAdmin()).thenReturn(1L);

        service.create(form(" Staff2@Example.TEST ", "matkhau123"));

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("staff2@example.test");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(AccountRole.ADMIN);
        assertThat(saved.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void normalAdminCannotCreateOrLockAdmins() {
        when(access.requireAdmin()).thenReturn(2L);

        assertThatThrownBy(() -> service.create(form("new@example.test", "matkhau123")))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Chỉ super admin");
        assertThatThrownBy(() -> service.lock(1L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Chỉ super admin");
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void emailThatAlreadyHasAnAccountIsRefused() {
        when(access.requireAdmin()).thenReturn(1L);
        when(accounts.existsByEmail("khach@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.create(form("khach@example.test", "matkhau123")))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đã có tài khoản");
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void superAdminCannotBeLocked() {
        when(access.requireAdmin()).thenReturn(1L);

        assertThatThrownBy(() -> service.lock(1L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("super admin");
        verify(accounts, never()).updateStatus(anyLong(), any(), any(), any());
    }

    @Test
    void superAdminLocksAndUnlocksAnotherAdmin() {
        when(access.requireAdmin()).thenReturn(1L);
        when(accounts.updateStatus(2L, AccountRole.ADMIN, AccountStatus.ACTIVE, AccountStatus.DISABLED)).thenReturn(1);
        when(accounts.updateStatus(2L, AccountRole.ADMIN, AccountStatus.DISABLED, AccountStatus.ACTIVE)).thenReturn(1);

        service.lock(2L);
        service.unlock(2L);

        verify(accounts).updateStatus(2L, AccountRole.ADMIN, AccountStatus.ACTIVE, AccountStatus.DISABLED);
        verify(accounts).updateStatus(2L, AccountRole.ADMIN, AccountStatus.DISABLED, AccountStatus.ACTIVE);
    }

    @Test
    void pageMarksTheSuperAdminAndOnlyLetsTheSuperAdminManage() {
        when(access.requireAdmin()).thenReturn(2L);

        AdminAccountsPageView page = service.page();

        assertThat(page.canManage()).isFalse();
        assertThat(page.rows().get(0).superAdmin()).isTrue();
        assertThat(page.rows().get(0).lockable()).isFalse();
        assertThat(page.rows().get(1).currentUser()).isTrue();
    }

    private Account admin(long id, AccountStatus status) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setEmail("admin" + id + "@example.test");
        account.setRole(AccountRole.ADMIN);
        account.setStatus(status);
        account.setCreatedAt(LocalDateTime.of(2026, 11, 1, 3, 0));
        return account;
    }

    private AdminAccountForm form(String email, String password) {
        AdminAccountForm form = new AdminAccountForm();
        form.setEmail(email);
        form.setPassword(password);
        return form;
    }
}
