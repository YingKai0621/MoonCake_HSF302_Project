package com.se201411.project.mooncake.cart.repository;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.enums.ConfigurationType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomBoxConfigurationRepository
    extends Repository<BoxConfiguration, Long> {

    BoxConfiguration save(BoxConfiguration configuration);

    void delete(BoxConfiguration configuration);

    @Query("""
        select configuration
        from BoxConfiguration configuration
        join fetch configuration.box box
        join fetch box.emptyBoxVariant boxVariant
        join fetch boxVariant.product boxProduct
        join fetch boxProduct.category boxCategory
        where configuration.id = :configurationId
          and configuration.cart.id = :cartId
          and configuration.type = :configurationType
        """)
    Optional<BoxConfiguration> findOwnedConfiguration(
        @Param("configurationId") long configurationId,
        @Param("cartId") long cartId,
        @Param("configurationType") ConfigurationType configurationType
    );
}
