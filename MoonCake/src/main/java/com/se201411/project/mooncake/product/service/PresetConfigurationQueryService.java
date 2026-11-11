package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;

import java.util.Set;

public interface PresetConfigurationQueryService {

    PresetRecipeSnapshot requireReadyPreset(long giftSetVariantId);

    Set<Long> readyGiftSetVariantIds();
}
