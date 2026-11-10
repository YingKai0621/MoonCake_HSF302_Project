package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.ProductVariant;

public record CustomCartComponentView(
    String name,
    String variantName,
    int quantityPerBox
) {

    /** Entity -> DTO */
    public static CustomCartComponentView entityToDto(BoxConfigurationItem component) {
        ProductVariant cake = component.getMooncakeVariant();
        return new CustomCartComponentView(
            cake.getProduct().getName(),
            cake.getName(),
            component.getQuantityPerBox()
        );
    }
}
