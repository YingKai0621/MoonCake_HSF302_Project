package com.se201411.project.mooncake.inventory.repository;

import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.enums.StockMovementType;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryAdminRepository {
    List<InventoryBalance> balances();

    List<StockMovement> movements(int page);

    boolean isGiftSet(long id);

    boolean hasStockOrHistory(long id);

    void lockOperation(String group);

    boolean operationExists(String group);

    int change(long id, int delta);

    void movement(long variant, long admin, int delta, StockMovementType type,
                  String reason, String group, String key, LocalDateTime now);
}
