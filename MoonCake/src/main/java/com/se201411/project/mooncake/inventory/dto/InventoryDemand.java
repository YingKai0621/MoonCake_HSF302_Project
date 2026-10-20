package com.se201411.project.mooncake.inventory.dto;

public record InventoryDemand(long variantId, int quantity) {

    public InventoryDemand {
        if (variantId <= 0) {
            throw new IllegalArgumentException("Variant ID must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inventory demand must be positive");
        }
    }
}
