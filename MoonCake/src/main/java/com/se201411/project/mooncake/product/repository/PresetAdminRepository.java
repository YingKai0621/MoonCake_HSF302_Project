package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import java.util.List;

public interface PresetAdminRepository {

    List<BoxConfiguration> all();

    BoxConfiguration lock(long id);

    List<BoxConfigurationItem> items(long id);

    List<BoxConfigurationItem> allPresetItems();

    boolean historicallyReferenced(long id, long variant);

    void save(BoxConfiguration c);

    void saveItem(BoxConfigurationItem i);

    void removeItem(BoxConfigurationItem i);
}
