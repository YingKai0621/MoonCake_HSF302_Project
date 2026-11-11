package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ComponentRole;

import java.math.BigDecimal;

public record CheckoutComponentQuote(
    long variantId,
    ComponentRole role,
    String name,
    int quantityPerSet,
    BigDecimal unitPrice
) {

    public static CheckoutComponentQuote entityToDto(ProductVariant variant, ComponentRole role, int quantityPerSet) {
        String name = variant.getProduct().getName() + " - " + variant.getName();
        return new CheckoutComponentQuote(variant.getId(), role, name, quantityPerSet, variant.getUnitPrice());
    }
}
