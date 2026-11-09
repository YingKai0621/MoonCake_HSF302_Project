package com.se201411.project.mooncake.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    StockMovement save(StockMovement movement);
}
