package com.se201411.project.mooncake.inventory.repository;

import com.se201411.project.mooncake.entity.StockMovement;
import org.springframework.data.repository.Repository;

public interface StockMovementRepository extends Repository<StockMovement, Long> {

    StockMovement save(StockMovement movement);
}
