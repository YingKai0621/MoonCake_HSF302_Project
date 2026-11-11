package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;

import java.util.Collection;
import java.util.Map;

public interface InventoryService {

    void initializeBalance(long variantId);

    Map<Long, Integer> availableQuantities(Collection<Long> variantIds);

    void requireBalance(long variantId);

    void deductForSale(SaleInventoryCommand command);

    void restoreForCancellation(CancellationInventoryCommand command);

    boolean hasStockRecords(Collection<Long> variantIds);

    void deleteBalances(Collection<Long> variantIds);
}
