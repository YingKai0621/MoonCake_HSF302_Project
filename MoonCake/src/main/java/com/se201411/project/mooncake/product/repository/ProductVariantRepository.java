package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.ProductVariant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends Repository<ProductVariant, Long> {

    @Query("""
        select v
        from ProductVariant v
        join fetch v.product p
        join fetch p.category c
        where v.active = true
          and p.active = true
          and c.active = true
          and p.id in :productIds
        order by v.unitPrice, v.id
        """)
    List<ProductVariant> findActiveByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("""
        select v
        from ProductVariant v
        join fetch v.product p
        join fetch p.category c
        where v.id = :variantId
          and v.active = true
          and p.active = true
          and c.active = true
        """)
    Optional<ProductVariant> findSellableById(@Param("variantId") long variantId);
}
