package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.enums.ProductType;

import java.math.BigDecimal;
import java.util.Objects;

public record CartItemView(
    long cartItemId,
    long variantId,
    String productSlug,
    String productName,
    String variantName,
    String sku,
    ProductType productType,
    String imageUrl,
    String imageAlt,
    int quantity,
    BigDecimal unitPrice,
    String formattedUnitPrice,
    BigDecimal lineTotal,
    String formattedLineTotal,
    boolean currentlySellable
) {
    public CartItemView {
        Objects.requireNonNull(productSlug, "productSlug must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(variantName, "variantName must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(productType, "productType must not be null");
        Objects.requireNonNull(imageUrl, "imageUrl must not be null");
        Objects.requireNonNull(imageAlt, "imageAlt must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(lineTotal, "lineTotal must not be null");
    }

    public String productUrl() {
        return "/products/" + productSlug;
    }
}
