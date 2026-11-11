package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
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

    /** Entity -> DTO for a SKU the service has already checked as sellable. */
    public static SellableVariantSnapshot entityToDto(ProductVariant variant) {
        Product product = variant.getProduct();
        return new SellableVariantSnapshot(
            variant.getId(),
            product.getId(),
            variant.getSku(),
            product.getProductType(),
            product.getName() + " - " + variant.getName(),
            variant.getUnitPrice(),
            true
        );
    }
}
