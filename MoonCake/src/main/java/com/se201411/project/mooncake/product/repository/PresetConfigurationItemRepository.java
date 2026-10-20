package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PresetConfigurationItemRepository extends Repository<BoxConfigurationItem, Long> {

    @Query("""
        select item
        from BoxConfigurationItem item
        join fetch item.configuration configuration
        join fetch item.mooncakeVariant variant
        join fetch variant.product product
        where configuration.id in :configurationIds
        order by configuration.id, product.name, variant.name, variant.id
        """)
    List<BoxConfigurationItem> findByConfigurationIds(
        @Param("configurationIds") Collection<Long> configurationIds
    );
}
