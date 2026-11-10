package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import java.util.Objects;

public record PresetComponentView(
    long variantId,
    String sku,
    String name,
    int quantityPerBox
) {

    /** Entity -> DTO. The service builds the cake display name and passes it in. */
    public static PresetComponentView entityToDto(BoxConfigurationItem item, String displayName) {
        ProductVariant cake = item.getMooncakeVariant();
        return new PresetComponentView(
            cake.getId(),
            cake.getSku(),
            displayName,
            item.getQuantityPerBox()
        );
    }

    public PresetComponentView {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (quantityPerBox <= 0) {
            throw new IllegalArgumentException("quantityPerBox must be positive");
        }
    }
}
