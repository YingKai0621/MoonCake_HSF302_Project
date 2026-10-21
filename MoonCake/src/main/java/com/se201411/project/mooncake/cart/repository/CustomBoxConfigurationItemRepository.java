package com.se201411.project.mooncake.cart.repository;

import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CustomBoxConfigurationItemRepository
    extends Repository<BoxConfigurationItem, Long> {

    BoxConfigurationItem save(BoxConfigurationItem item);

    void deleteAll(Iterable<? extends BoxConfigurationItem> items);

    @Query("""
        select item
        from BoxConfigurationItem item
        join fetch item.configuration configuration
        join fetch item.mooncakeVariant variant
        join fetch variant.product product
        join fetch product.category category
        where configuration.id in :configurationIds
        order by configuration.id, item.id
        """)
    List<BoxConfigurationItem> findByConfigurationIds(
        @Param("configurationIds") Collection<Long> configurationIds
    );

}
