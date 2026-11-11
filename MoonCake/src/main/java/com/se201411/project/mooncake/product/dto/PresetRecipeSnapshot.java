package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.enums.ConfigurationStatus;

import java.util.List;

public record PresetRecipeSnapshot(
    long configurationId,
    long giftSetVariantId,
    long boxVariantId,
    int capacityCakes,
    ConfigurationStatus status,
    List<PresetComponentSnapshot> components
) {

    public static PresetRecipeSnapshot entityToDto(
        BoxConfiguration configuration,
        long giftSetVariantId,
        List<PresetComponentSnapshot> components
    ) {
        return new PresetRecipeSnapshot(
            configuration.getId(),
            giftSetVariantId,
            configuration.getBox().getEmptyBoxVariant().getId(),
            configuration.getBox().getCapacityCakes(),
            configuration.getStatus(),
            components
        );
    }

    public PresetRecipeSnapshot {
        components = List.copyOf(components);
    }
}
