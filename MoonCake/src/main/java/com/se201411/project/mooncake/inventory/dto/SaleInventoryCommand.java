package com.se201411.project.mooncake.inventory.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SaleInventoryCommand(
    long orderId,
    String operationKey,
    List<InventoryDemand> demands
) {
    public SaleInventoryCommand {
        validate(orderId, operationKey, demands);
        demands = List.copyOf(demands);
    }

    private static void validate(long orderId, String operationKey, List<InventoryDemand> demands) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
        if (operationKey == null || operationKey.isBlank()) {
            throw new IllegalArgumentException("Operation key is required");
        }
        if (demands == null || demands.isEmpty()) {
            throw new IllegalArgumentException("At least one inventory demand is required");
        }
        Set<Long> variants = new HashSet<>();
        for (InventoryDemand demand : demands) {
            if (demand == null || !variants.add(demand.variantId())) {
                throw new IllegalArgumentException("Inventory demands must contain unique variants");
            }
        }
    }
}
