package com.se201411.project.mooncake.cart.repository;

import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.enums.ProductType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomBoxRepository extends Repository<Box, Long> {

    @Query("""
        select b
        from Box b
        join fetch b.emptyBoxVariant v
        join fetch v.product p
        join fetch p.category c
        where b.active = true
          and v.active = true
          and p.active = true
          and c.active = true
          and p.productType = :productType
        order by b.capacityCakes, b.id
        """)
    List<Box> findSellableBoxes(@Param("productType") ProductType productType);

    @Query("""
        select b
        from Box b
        join fetch b.emptyBoxVariant v
        join fetch v.product p
        join fetch p.category c
        where b.id = :boxId
          and b.active = true
          and v.active = true
          and p.active = true
          and c.active = true
          and p.productType = :productType
        """)
    Optional<Box> findSellableBoxById(
        @Param("boxId") long boxId,
        @Param("productType") ProductType productType
    );
}
