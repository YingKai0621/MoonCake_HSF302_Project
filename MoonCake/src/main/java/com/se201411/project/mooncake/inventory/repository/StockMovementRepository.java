package com.se201411.project.mooncake.inventory.repository;

import java.util.List;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    StockMovement save(StockMovement movement);

    @Query("select count(movement) from StockMovement movement where movement.variant.id in :variantIds")
    long countByVariantIds(@Param("variantIds") Collection<Long> variantIds);

    @Query("""
        select movement.performedByAccount.id
        from StockMovement movement
        where movement.order.id = :orderId
          and movement.movementType = :movementType
          and movement.performedByAccount is not null
        """)
    List<Long> findPerformerIds(
        @Param("orderId") long orderId,
        @Param("movementType") StockMovementType movementType
    );
}
