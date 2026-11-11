package com.se201411.project.mooncake.inventory.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    StockMovement save(StockMovement movement);

    @Query("select count(movement) from StockMovement movement where movement.variant.id in :variantIds")
    long countByVariantIds(@Param("variantIds") Collection<Long> variantIds);
}
