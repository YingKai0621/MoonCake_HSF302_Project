package com.se201411.project.mooncake.cart.dto;

import java.math.BigDecimal;

public record CustomCakeOptionView(
    long variantId,
    String productName,
    String variantName,
    String sku,
    BigDecimal unitPrice,
    String formattedPrice,
    String imageUrl,
    String imageAlt
) {
}
