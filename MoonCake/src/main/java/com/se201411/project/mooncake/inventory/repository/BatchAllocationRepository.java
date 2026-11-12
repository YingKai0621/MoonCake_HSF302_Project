package com.se201411.project.mooncake.inventory.repository;

import com.se201411.project.mooncake.entity.BatchAllocation;
import com.se201411.project.mooncake.entity.StockBatch;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.inventory.dto.BatchQuantity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BatchAllocationRepository extends JpaRepository<BatchAllocation, Long> {

    @Query("""
        select new com.se201411.project.mooncake.inventory.dto.BatchQuantity(
            allocation.batch.id, -sum(allocation.quantityDelta)
        )
        from BatchAllocation allocation
        where allocation.orderId = :orderId
          and allocation.batch.variantId = :variantId
          and allocation.quantityDelta < 0
        group by allocation.batch.id
        """)
    List<BatchQuantity> findSoldForOrder(
        @Param("variantId") long variantId,
        @Param("orderId") long orderId
    );

    @Query("""
        select min(source.expiresOn)
        from BatchAllocation allocation
        join allocation.batch source
        where locate(:prefix, allocation.operationKey) = 1
          and allocation.quantityDelta < 0
          and source.variant.product.productType = :productType
        """)
    LocalDate findEarliestExpiryConsumed(
        @Param("prefix") String prefix,
        @Param("productType") ProductType productType
    );

    @Query("""
        select distinct concat(source.variant.product.name, ' · ', source.variant.name)
        from BatchAllocation allocation
        join allocation.batch source
        where locate(:prefix, allocation.operationKey) = 1
          and allocation.quantityDelta < 0
          and source.variant.product.productType = :productType
          and source.expiresOn is null
        """)
    List<String> findUndatedConsumed(
        @Param("prefix") String prefix,
        @Param("productType") ProductType productType
    );

    @Query("""
        select allocation.batch
        from BatchAllocation allocation
        where allocation.operationKey = :operationKey
          and allocation.quantityDelta > 0
        """)
    List<StockBatch> findBatchesCreatedBy(@Param("operationKey") String operationKey);
}
