package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.ProductVariant;
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

    public static CustomBoxOptionView entityToDto(Box box, String imageUrl, String imageAlt) {
        ProductVariant variant = box.getEmptyBoxVariant();
        return new CustomBoxOptionView(
            box.getId(),
            variant.getId(),
            variant.getProduct().getName() + " · " + variant.getName(),
            variant.getSku(),
            box.getCapacityCakes(),
            variant.getUnitPrice(),
            MoneyFormat.vnd(variant.getUnitPrice()),
            imageUrl,
            imageAlt
        );
    }
}
