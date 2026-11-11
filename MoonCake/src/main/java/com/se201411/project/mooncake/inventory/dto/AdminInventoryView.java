package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.enums.StockMovementType;

import java.time.LocalDateTime;

public final class AdminInventoryView {
    private AdminInventoryView() {
    }

    public record Balance(long variantId, String sku, String productName, String name, int quantity, int threshold) {

        public static Balance entityToDto(InventoryBalance balance) {
            return new Balance(
                balance.getVariantId(),
                balance.getVariant().getSku(),
                balance.getVariant().getProduct().getName(),
                balance.getVariant().getName(),
                balance.getAvailableQuantity(),
                balance.getLowStockThreshold()
            );
        }

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

        public static Movement entityToDto(StockMovement movement, String typeLabel, String formattedCreatedAt) {
            Long orderId = null;
            if (movement.getOrder() != null) {
                orderId = movement.getOrder().getId();
            }
            ProductVariant variant = movement.getVariant();
            return new Movement(
                variant.getSku(),
                movement.getMovementType(),
                movement.getQuantityDelta(),
                movement.getReason(),
                movement.getOperationGroupKey(),
                orderId,
                movement.getCreatedAt(),
                variant.getProduct().getName() + " · " + variant.getName(),
                typeLabel,
                formattedCreatedAt
            );
        }
    }

    public record StockTab(StockFilter filter, String label, long count, boolean selected) {
    }

    public enum StockFilter {
        ALL,
        LOW,
        OUT
    }
}
