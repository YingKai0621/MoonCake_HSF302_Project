package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.entity.SalesOrder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record CancellationInventoryCommand(
    SalesOrder order,
    String operationKey,
    List<InventoryDemand> demands
) {
    public CancellationInventoryCommand {
        validate(order, operationKey, demands);
        demands = List.copyOf(demands);
    }

    private static void validate(SalesOrder order, String operationKey, List<InventoryDemand> demands) {
        if (order == null || order.getId() == null || order.getId() <= 0) {
            throw new IllegalArgumentException("A saved order is required");
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
