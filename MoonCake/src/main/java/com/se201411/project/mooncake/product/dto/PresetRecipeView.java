package com.se201411.project.mooncake.product.dto;

import java.util.List;
import java.util.Objects;

public record PresetRecipeView(
    long configurationId,
    long giftSetVariantId,
    long boxVariantId,
    String boxVariantName,
    int capacityCakes,
    List<PresetComponentView> components
) {
    public PresetRecipeView {
        Objects.requireNonNull(boxVariantName, "boxVariantName must not be null");
        if (capacityCakes <= 0) {
            throw new IllegalArgumentException("capacityCakes must be positive");
        }
        components = List.copyOf(components);
    }
}
