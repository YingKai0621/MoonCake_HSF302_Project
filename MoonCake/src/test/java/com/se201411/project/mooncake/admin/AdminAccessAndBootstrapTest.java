package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.security.service.*;
import com.se201411.project.mooncake.security.repository.*;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AdminAccessAndBootstrapTest {
    AccountRepository accounts = mock(AccountRepository.class);
    AdminAccessService access = new AdminAccessService(accounts);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnonymousAndCustomer() {
        assertThatThrownBy(() -> access.requireAdmin()).isInstanceOf(AccessDeniedException.class);
        login(AccountRole.CUSTOMER);
        assertThatThrownBy(() -> access.requireAdmin()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(accounts);
    }

    @Test
    void disabledAdminSessionIsRejectedUsingCurrentDatabaseStatus() {
        login(AccountRole.ADMIN);
        var a = new Account();
        ReflectionTestUtils.setField(a, "id", 1L);
        a.setRole(AccountRole.ADMIN);
        a.setStatus(AccountStatus.DISABLED);
        when(accounts.findByEmail("admin@example.test")).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> access.requireAdmin()).isInstanceOf(AccessDeniedException.class);
        a.setStatus(AccountStatus.ACTIVE);
        assertThat(access.requireAdmin()).isEqualTo(1);
    }

    @Test
    void existingAdminDoesNotChangeCredentials() {
        var repo = mock(AdminBootstrapRepository.class);
        var encoder = mock(PasswordEncoder.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AdminBootstrapService(repo, encoder, factory.getValidator(), Clock.systemUTC());
            when(repo.adminExistsUnderLock()).thenReturn(true);
            assertThat(service.createIfAbsent(null, null)).isFalse();
            verifyNoInteractions(encoder);
            verify(repo, never()).save(any());
        }
    }

    @Test
    void bootstrapNormalizesAndHashesWithoutPromotingExistingCustomer() {
        var repo = mock(AdminBootstrapRepository.class);
        var encoder = mock(PasswordEncoder.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AdminBootstrapService(repo, encoder, factory.getValidator(), Clock.systemUTC());
            when(repo.emailExists("admin@example.test")).thenReturn(true);
            assertThatThrownBy(() -> service.createIfAbsent(" ADMIN@example.test ", "test-only-password")).isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(encoder);
            when(repo.emailExists("admin@example.test")).thenReturn(false);
            when(encoder.encode(any())).thenReturn("test-hash");
            assertThat(service.createIfAbsent(" ADMIN@example.test ", "test-only-password")).isTrue();
            verify(repo).save(argThat(a -> a.getRole() == AccountRole.ADMIN && a.getStatus() == AccountStatus.ACTIVE && a.getEmail().equals("admin@example.test") && a.getPasswordHash().equals("test-hash")));
        }
    }

    @Test
    void bootstrapRejectsPasswordOverBcryptByteLimit() {
        var repo = mock(AdminBootstrapRepository.class);
        var encoder = mock(PasswordEncoder.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AdminBootstrapService(repo, encoder, factory.getValidator(), Clock.systemUTC());
            assertThatThrownBy(() -> service.createIfAbsent("admin@example.test", "密".repeat(30))).isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(encoder);
        }
    }

    void login(AccountRole role) {
        var p = new MoonCakeUserPrincipal(1, null, "admin@example.test", "", role, AccountStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    }
}
