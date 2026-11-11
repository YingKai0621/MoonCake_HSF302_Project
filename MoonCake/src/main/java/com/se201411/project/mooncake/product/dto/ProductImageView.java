package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.ProductImage;

public record ProductImageView(
    String url,
    String altText,
    long variantId
) {

    public static ProductImageView entityToDto(ProductImage image) {
        return new ProductImageView(
            image.getImageUrl(),
            image.getAltText(),
            image.getVariant().getId()
        );
    }
}
