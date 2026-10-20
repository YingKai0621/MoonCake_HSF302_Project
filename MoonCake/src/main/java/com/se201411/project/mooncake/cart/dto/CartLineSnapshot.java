package com.se201411.project.mooncake.cart.dto;

public record CartLineSnapshot(Long variantId, Long configurationId, int quantity) {

    public CartLineSnapshot {
        if ((variantId == null) == (configurationId == null)) {
            throw new IllegalArgumentException("Cart line requires exactly one purchase target");
        }
        if (variantId != null && variantId <= 0) {
            throw new IllegalArgumentException("Variant ID must be positive");
        }
        if (configurationId != null && configurationId <= 0) {
            throw new IllegalArgumentException("Configuration ID must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
