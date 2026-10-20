package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.enums.ComponentRole;

import java.math.BigDecimal;

public record CheckoutComponentQuote(
    long variantId,
    ComponentRole role,
    String name,
    int quantityPerSet,
    BigDecimal unitPrice
) {
}
