package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.*;

import java.util.List;

public interface PresetConfigurationAdminService {
    List<AdminPresetView> presets();

    void saveDraft(AdminPresetForm form);

    void setComponent(long configurationId, long variantId, int quantity);

    void activate(long configurationId);
}
