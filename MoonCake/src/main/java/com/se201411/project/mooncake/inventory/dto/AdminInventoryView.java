package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.enums.StockMovementType;

import java.time.LocalDateTime;

public final class AdminInventoryView {
    private AdminInventoryView() {
    }

    public record Balance(long variantId, String sku, String productName, String name, int quantity, int threshold) {
        public boolean low() {
            return quantity <= threshold;
        }

        public boolean out() {
            return quantity <= 0;
        }

        public String statusLabel() {
            if (out()) {
                return "Hết hàng";
            }
            if (low()) {
                return "Sắp hết";
            }
            return "Đủ hàng";
        }

        public String statusTone() {
            if (out()) {
                return "danger";
            }
            if (low()) {
                return "warning";
            }
            return "success";
        }
    }

    public record Movement(String sku, StockMovementType type, int delta, String reason,
                           String groupKey, Long orderId, LocalDateTime createdAt,
                           String productName, String typeLabel, String formattedCreatedAt) {
    }

    public record StockTab(StockFilter filter, String label, long count, boolean selected) {
    }

    public enum StockFilter {
        ALL,
        LOW,
        OUT
    }
}
