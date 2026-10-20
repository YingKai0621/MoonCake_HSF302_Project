package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.enums.ProductType;

import java.math.BigDecimal;

public record SellableVariantSnapshot(
    long variantId,
    long productId,
    String sku,
    ProductType productType,
    String displayName,
    BigDecimal unitPrice,
    boolean active
) {
}
