package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.enums.ProductType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record CartItemView(
    long cartItemId,
    Long variantId,
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
    boolean currentlySellable,
    Long configurationId,
    boolean custom,
    int capacityCakes,
    List<CustomCartComponentView> components
) {
    public CartItemView(
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
        this(
            cartItemId,
            variantId,
            productSlug,
            productName,
            variantName,
            sku,
            productType,
            imageUrl,
            imageAlt,
            quantity,
            unitPrice,
            formattedUnitPrice,
            lineTotal,
            formattedLineTotal,
            currentlySellable,
            null,
            false,
            0,
            List.of()
        );
    }

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
        components = List.copyOf(components);
    }

    public String productUrl() {
        if (custom) {
            return "/custom-boxes/" + configurationId + "/edit";
        }
        return "/products/" + productSlug;
    }

    public static CartItemView custom(
        long cartItemId,
        long configurationId,
        String productName,
        String variantName,
        String boxSku,
        String imageUrl,
        String imageAlt,
        int quantity,
        BigDecimal unitPrice,
        String formattedUnitPrice,
        BigDecimal lineTotal,
        String formattedLineTotal,
        boolean currentlySellable,
        int capacityCakes,
        List<CustomCartComponentView> components
    ) {
        return new CartItemView(
            cartItemId,
            null,
            "",
            productName,
            variantName,
            boxSku,
            ProductType.GIFT_SET,
            imageUrl,
            imageAlt,
            quantity,
            unitPrice,
            formattedUnitPrice,
            lineTotal,
            formattedLineTotal,
            currentlySellable,
            configurationId,
            true,
            capacityCakes,
            components
        );
    }
}
