package com.se201411.project.mooncake.security.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopperIdentityTest {

    @Test
    void acceptsExactlyOneOwnerType() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(7L);
        ShopperIdentity guest = ShopperIdentity.forGuest("opaque-guest-token");

        assertThat(customer.isCustomer()).isTrue();
        assertThat(guest.isCustomer()).isFalse();
        assertThat(guest.toString()).doesNotContain("opaque-guest-token");
    }

    @Test
    void rejectsMissingOrAmbiguousOwner() {
        assertThatThrownBy(() -> new ShopperIdentity(null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShopperIdentity(1L, "guest"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
