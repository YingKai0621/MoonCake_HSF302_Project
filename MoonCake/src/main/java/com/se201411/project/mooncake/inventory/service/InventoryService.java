package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;

public interface InventoryService {

    void initializeBalance(long variantId);

    void requireBalance(long variantId);

    void deductForSale(SaleInventoryCommand command);

    void restoreForCancellation(CancellationInventoryCommand command);
}
