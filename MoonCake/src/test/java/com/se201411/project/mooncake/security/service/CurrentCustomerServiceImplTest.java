package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.common.exception.OwnershipMismatchException;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.impl.CurrentCustomerServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentCustomerServiceImplTest {

    private final CurrentCustomerServiceImpl service = new CurrentCustomerServiceImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCustomerIdentityFromAuthenticatedPrincipal() {
        authenticate(principal(10L, 20L, AccountRole.CUSTOMER));

        ShopperIdentity identity = service.requireCurrentCustomer();

        assertThat(identity.customerId()).isEqualTo(20L);
    }

    @Test
    void rejectsAdminPrincipal() {
        authenticate(principal(10L, null, AccountRole.ADMIN));

        assertThatThrownBy(service::requireCurrentCustomer)
            .isInstanceOf(OwnershipMismatchException.class);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatThrownBy(service::requireCurrentCustomer)
            .isInstanceOf(OwnershipMismatchException.class);
    }

    private void authenticate(MoonCakeUserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MoonCakeUserPrincipal principal(long accountId, Long customerId, AccountRole role) {
        return new MoonCakeUserPrincipal(
            accountId,
            customerId,
            role == AccountRole.ADMIN ? "admin@example.com" : "customer@example.com",
            "encoded-password",
            role,
            AccountStatus.ACTIVE
        );
    }
}
