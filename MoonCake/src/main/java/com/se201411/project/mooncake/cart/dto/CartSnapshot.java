package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.security.dto.ShopperIdentity;

import java.util.List;

public record CartSnapshot(
    long cartId,
    ShopperIdentity shopper,
    List<CartLineSnapshot> lines
) {
    public CartSnapshot {
        lines = List.copyOf(lines);
    }
}
