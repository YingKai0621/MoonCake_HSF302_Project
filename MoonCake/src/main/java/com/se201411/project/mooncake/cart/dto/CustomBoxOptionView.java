package com.se201411.project.mooncake.cart.dto;

import java.math.BigDecimal;

public record CustomBoxOptionView(
    long boxId,
    long variantId,
    String name,
    String sku,
    int capacityCakes,
    BigDecimal unitPrice,
    String formattedPrice,
    String imageUrl,
    String imageAlt
) {
}
