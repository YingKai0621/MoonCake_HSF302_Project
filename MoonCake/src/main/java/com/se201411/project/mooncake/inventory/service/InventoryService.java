package com.se201411.project.mooncake.inventory.service;

import java.util.Collection;
import java.util.Map;

import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;

public interface InventoryService {

    void initializeBalance(long variantId);

    Map<Long, Integer> availableQuantities(Collection<Long> variantIds);

    void requireBalance(long variantId);

    void deductForSale(SaleInventoryCommand command);

    void restoreForCancellation(CancellationInventoryCommand command);

    boolean hasStockRecords(Collection<Long> variantIds);

    void deleteBalances(Collection<Long> variantIds);
}
