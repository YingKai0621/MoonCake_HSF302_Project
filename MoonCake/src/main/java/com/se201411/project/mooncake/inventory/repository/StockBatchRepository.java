package com.se201411.project.mooncake.inventory.repository;

import com.se201411.project.mooncake.entity.StockBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    @Query("""
        select coalesce(sum(batch.remainingQuantity), 0)
        from StockBatch batch
        where batch.variantId = :variantId
        """)
    long sumRemaining(@Param("variantId") long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select batch
        from StockBatch batch
        where batch.variantId = :variantId
          and batch.remainingQuantity > 0
          and (batch.expiresOn is null or batch.expiresOn >= :today)
        order by case when batch.expiresOn is null then 1 else 0 end,
            batch.expiresOn,
            batch.id
        """)
    List<StockBatch> findSellableForUpdate(
        @Param("variantId") long variantId,
        @Param("today") LocalDate today
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select batch
        from StockBatch batch
        where batch.variantId = :variantId
          and batch.remainingQuantity > 0
        order by case when batch.expiresOn is null then 1 else 0 end,
            batch.expiresOn,
            batch.id
        """)
    List<StockBatch> findAllWithStockForUpdate(@Param("variantId") long variantId);

    @Modifying(flushAutomatically = true)
    @Query("""
        update StockBatch batch
        set batch.remainingQuantity = batch.remainingQuantity + :delta
        where batch.id = :batchId
          and batch.remainingQuantity >= :minimumBefore
          and batch.remainingQuantity <= :maximumBefore
        """)
    int changeRemaining(
        @Param("batchId") long batchId,
        @Param("delta") int delta,
        @Param("minimumBefore") int minimumBefore,
        @Param("maximumBefore") int maximumBefore
    );

    @EntityGraph(attributePaths = "variant")
    List<StockBatch> findAllByOrderByIdDesc();

    @Modifying(flushAutomatically = true)
    @Query("""
        update StockBatch batch
        set batch.batchCode = :code,
            batch.producedOn = :producedOn,
            batch.expiresOn = :expiresOn
        where batch.id = :batchId
          and not exists (
            select allocation.id
            from BatchAllocation allocation
            where allocation.batch.id = :batchId
              and allocation.quantityDelta < 0
          )
        """)
    int updateTraceability(
        @Param("batchId") long batchId,
        @Param("code") String code,
        @Param("producedOn") LocalDate producedOn,
        @Param("expiresOn") LocalDate expiresOn
    );

    @Query("select count(batch) from StockBatch batch where batch.variantId in :variantIds")
    long countByVariantIds(@Param("variantIds") Collection<Long> variantIds);

    @Query("""
        select batch
        from StockBatch batch
        where batch.variantId in :variantIds
          and batch.remainingQuantity > 0
          and (batch.expiresOn is null or batch.expiresOn >= :today)
        order by batch.variantId,
            case when batch.expiresOn is null then 1 else 0 end,
            batch.expiresOn,
            batch.id
        """)
    List<StockBatch> findSellableBatches(
        @Param("variantIds") Collection<Long> variantIds,
        @Param("today") LocalDate today
    );
}
