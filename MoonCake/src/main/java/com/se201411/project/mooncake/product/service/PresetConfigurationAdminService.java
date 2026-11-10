package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.AdminPresetForm;
import com.se201411.project.mooncake.product.dto.AdminPresetOptions;
import com.se201411.project.mooncake.product.dto.AdminPresetView;
import java.util.List;
import java.util.Map;

public interface PresetConfigurationAdminService {
    List<AdminPresetView> presets();

    AdminPresetOptions presetOptions();

    List<AdminPresetView> presetsForProduct(long productId);

    void saveDraft(AdminPresetForm form);

    void setComponent(long configurationId, long variantId, int quantity);

    void setComponents(long configurationId, Map<Long, Integer> quantities);

    void activate(long configurationId);

    boolean canUnlock(long configurationId);

    void unlock(long configurationId);

    void copyRecipe(long fromConfigurationId, long toGiftSetVariantId);
}
