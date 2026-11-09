package com.se201411.project.mooncake.product.dto;

import java.math.BigDecimal;

public record AdminProductRow(
    long id,
    String name,
    String categoryName,
    String typeLabel,
    int skuCount,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String imageUrl,
    boolean active
) {
}
