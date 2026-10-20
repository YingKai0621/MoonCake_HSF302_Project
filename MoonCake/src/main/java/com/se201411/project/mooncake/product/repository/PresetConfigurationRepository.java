package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PresetConfigurationRepository extends Repository<BoxConfiguration, Long> {

    @Query("""
        select configuration
        from BoxConfiguration configuration
        join fetch configuration.box box
        join fetch box.emptyBoxVariant boxVariant
        join fetch configuration.presetVariant giftSetVariant
        where configuration.type = :type
          and configuration.status = :status
          and giftSetVariant.id in :giftSetVariantIds
        """)
    List<BoxConfiguration> findReadyByGiftSetVariantIds(
        @Param("giftSetVariantIds") Collection<Long> giftSetVariantIds,
        @Param("type") ConfigurationType type,
        @Param("status") ConfigurationStatus status
    );

    @Query("""
        select configuration
        from BoxConfiguration configuration
        join fetch configuration.box box
        join fetch box.emptyBoxVariant boxVariant
        join fetch configuration.presetVariant giftSetVariant
        where configuration.type = :type
          and configuration.status = :status
          and giftSetVariant.id = :giftSetVariantId
        """)
    Optional<BoxConfiguration> findReadyByGiftSetVariantId(
        @Param("giftSetVariantId") long giftSetVariantId,
        @Param("type") ConfigurationType type,
        @Param("status") ConfigurationStatus status
    );
}
