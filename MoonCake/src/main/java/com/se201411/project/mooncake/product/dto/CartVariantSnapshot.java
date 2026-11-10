package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ProductType;

import java.math.BigDecimal;
import java.util.Objects;

public record CartVariantSnapshot(
    long variantId,
    String productSlug,
    String productName,
    String variantName,
    String sku,
    ProductType productType,
    BigDecimal unitPrice,
    String imageUrl,
    String imageAlt,
    boolean currentlySellable,
    int stock
) {

    /** Entity -> DTO. Picture, sellable flag and stock are worked out by the service. */
    public static CartVariantSnapshot entityToDto(
        ProductVariant variant,
        String imageUrl,
        String imageAlt,
        boolean currentlySellable,
        int availableQuantity
    ) {
        Product product = variant.getProduct();
        return new CartVariantSnapshot(
            variant.getId(),
            product.getSlug(),
            product.getName(),
            variant.getName(),
            variant.getSku(),
            product.getProductType(),
            variant.getUnitPrice(),
            imageUrl,
            imageAlt,
            currentlySellable,
            availableQuantity
        );
    }

    public CartVariantSnapshot(long variantId, String productSlug, String productName, String variantName,
                               String sku, ProductType productType, BigDecimal unitPrice, String imageUrl,
                               String imageAlt, boolean currentlySellable) {
        this(variantId, productSlug, productName, variantName, sku, productType, unitPrice, imageUrl,
            imageAlt, currentlySellable, -1);
    }

    public CartVariantSnapshot {
        if (variantId <= 0) {
            throw new IllegalArgumentException("Variant ID must be positive");
        }
        Objects.requireNonNull(productSlug, "productSlug must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(variantName, "variantName must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(productType, "productType must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(imageUrl, "imageUrl must not be null");
        Objects.requireNonNull(imageAlt, "imageAlt must not be null");
    }

    public String displayName() {
        return productName + " - " + variantName;
    }
}
