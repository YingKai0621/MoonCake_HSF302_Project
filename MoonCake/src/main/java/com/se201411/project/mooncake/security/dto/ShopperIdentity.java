package com.se201411.project.mooncake.security.dto;

public record ShopperIdentity(Long customerId, String guestToken) {

    public ShopperIdentity {
        boolean hasCustomer = customerId != null;
        boolean hasGuest = guestToken != null && !guestToken.isBlank();
        if (hasCustomer == hasGuest) {
            throw new IllegalArgumentException("Shopper identity requires exactly one owner type");
        }
        if (hasCustomer && customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
    }

    public static ShopperIdentity forCustomer(long customerId) {
        return new ShopperIdentity(customerId, null);
    }

    public static ShopperIdentity forGuest(String guestToken) {
        return new ShopperIdentity(null, guestToken);
    }

    public boolean isCustomer() {
        return customerId != null;
    }

    @Override
    public String toString() {
        return isCustomer()
            ? "ShopperIdentity[customerId=" + customerId + "]"
            : "ShopperIdentity[guestToken=REDACTED]";
    }
}
