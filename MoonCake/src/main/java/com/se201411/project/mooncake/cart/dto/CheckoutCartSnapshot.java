package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.security.dto.ShopperIdentity;

import java.util.List;

public record CheckoutCartSnapshot(
    long cartId,
    ShopperIdentity customerIdentity,
    List<CartLineSnapshot> lines
) {
    public CheckoutCartSnapshot {
        lines = List.copyOf(lines);
    }
}
