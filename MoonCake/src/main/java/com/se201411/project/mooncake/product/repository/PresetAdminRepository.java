package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;

import java.util.List;

public interface PresetAdminRepository {

    List<BoxConfiguration> all();

    BoxConfiguration lock(long id);

    List<BoxConfigurationItem> items(long id);

    List<BoxConfigurationItem> allPresetItems();

    void save(BoxConfiguration configuration);

    void saveItem(BoxConfigurationItem item);

    void removeItem(BoxConfigurationItem item);
}
