package com.se201411.project.mooncake.security.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopperIdentityTest {

    @Test
    void acceptsPositiveCustomerId() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(7L);

        assertThat(customer.customerId()).isEqualTo(7L);
    }

    @Test
    void rejectsNonPositiveCustomerId() {
        assertThatThrownBy(() -> new ShopperIdentity(0L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
