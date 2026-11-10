package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.BoxConfigurationItem;

public record PresetComponentSnapshot(long variantId, int quantityPerBox) {

    /** Entity -> DTO */
    public static PresetComponentSnapshot entityToDto(BoxConfigurationItem item) {
        return new PresetComponentSnapshot(
            item.getMooncakeVariant().getId(),
            item.getQuantityPerBox()
        );
    }
}
