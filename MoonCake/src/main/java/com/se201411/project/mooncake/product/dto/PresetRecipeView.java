package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.ProductVariant;
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

    /** Entity -> DTO. The service maps the recipe lines and passes them in. */
    public static PresetRecipeView entityToDto(BoxConfiguration preset, List<PresetComponentView> components) {
        ProductVariant boxVariant = preset.getBox().getEmptyBoxVariant();
        return new PresetRecipeView(
            preset.getId(),
            preset.getPresetVariant().getId(),
            boxVariant.getId(),
            boxVariant.getName(),
            preset.getBox().getCapacityCakes(),
            components
        );
    }

    public PresetRecipeView {
        Objects.requireNonNull(boxVariantName, "boxVariantName must not be null");
        if (capacityCakes <= 0) {
            throw new IllegalArgumentException("capacityCakes must be positive");
        }
        components = List.copyOf(components);
    }
}
