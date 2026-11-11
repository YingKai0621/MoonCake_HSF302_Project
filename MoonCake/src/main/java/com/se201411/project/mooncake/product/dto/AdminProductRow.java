package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.Product;
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
    String webLabel,
    String webTone
) {

    public static AdminProductRow entityToDto(
        Product product,
        String typeLabel,
        int skuCount,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String imageUrl,
        String webLabel,
        String webTone
    ) {
        return new AdminProductRow(
            product.getId(),
            product.getName(),
            product.getCategory().getName(),
            typeLabel,
            skuCount,
            minPrice,
            maxPrice,
            imageUrl,
            webLabel,
            webTone
        );
    }
}
