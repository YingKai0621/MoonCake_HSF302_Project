package com.se201411.project.mooncake.inventory.dto;

import java.time.LocalDateTime;

import com.se201411.project.mooncake.enums.StockMovementType;

public final class AdminInventoryView {
    private AdminInventoryView() {
    }

    public record Balance(long variantId, String sku, String name, int quantity, int threshold) {
        public boolean low() {
            return quantity <= threshold;
        }
    }

    public record Movement(String sku, StockMovementType type, int delta, String reason,
                           String groupKey, Long orderId, LocalDateTime createdAt) {
    }
}
