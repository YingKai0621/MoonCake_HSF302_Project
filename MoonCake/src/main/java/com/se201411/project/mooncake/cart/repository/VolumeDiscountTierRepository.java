package com.se201411.project.mooncake.cart.repository;

import com.se201411.project.mooncake.entity.VolumeDiscountTier;
import com.se201411.project.mooncake.enums.DiscountGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VolumeDiscountTierRepository
    extends Repository<VolumeDiscountTier, Long> {

    @Query("""
        select tier
        from VolumeDiscountTier tier
        where tier.active = true
          and tier.discountGroup = :discountGroup
          and tier.minQuantity <= :quantity
          and (tier.maxQuantity is null or tier.maxQuantity >= :quantity)
          and (tier.validFrom is null or tier.validFrom <= :now)
          and (tier.validTo is null or tier.validTo > :now)
        order by tier.minQuantity desc
        """)
    List<VolumeDiscountTier> findApplicableTiers(
        @Param("discountGroup") DiscountGroup discountGroup,
        @Param("quantity") int quantity,
        @Param("now") LocalDateTime now
    );
}
