package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;

public interface PresetConfigurationQueryService {

    /**
     * Returns the immutable READY recipe associated with a GIFT_SET SKU.
     */
    PresetRecipeSnapshot requireReadyPreset(long giftSetVariantId);
}
