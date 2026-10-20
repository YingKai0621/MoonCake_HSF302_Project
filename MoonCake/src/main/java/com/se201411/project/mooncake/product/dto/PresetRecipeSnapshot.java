package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.enums.ConfigurationStatus;

import java.util.List;

public record PresetRecipeSnapshot(
    long configurationId,
    long giftSetVariantId,
    long boxVariantId,
    ConfigurationStatus status,
    List<PresetComponentSnapshot> components
) {
    public PresetRecipeSnapshot {
        components = List.copyOf(components);
    }
}
