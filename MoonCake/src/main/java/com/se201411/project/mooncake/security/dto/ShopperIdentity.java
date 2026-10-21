package com.se201411.project.mooncake.security.dto;

public record ShopperIdentity(long customerId) {

    public ShopperIdentity {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
    }

    public static ShopperIdentity forCustomer(long customerId) {
        return new ShopperIdentity(customerId);
    }
}
