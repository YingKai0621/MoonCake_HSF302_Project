package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.enums.ComponentRole;
import com.se201411.project.mooncake.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record AdminOrderView(
    long id,
    String code,
    OrderStatus status,
    String statusLabel,
    String statusTone,
    String recipient,
    String phone,
    String address,
    String note,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal shipping,
    BigDecimal roundingAdjustment,
    BigDecimal total,
    String formattedCreatedAt,
    String formattedUpdatedAt,
    List<Item> items,
    List<PaymentLine> payments,
    Actions actions
) {
    public AdminOrderView {
        items = List.copyOf(items);
        payments = List.copyOf(payments);
    }

    public record Item(
        String name,
        String sku,
        int quantity,
        BigDecimal price,
        BigDecimal total,
        List<Component> components
    ) {

        /** Entity -> DTO. The service maps the item's components and passes them in. */
        public static Item entityToDto(OrderItem item, List<Component> components) {
            return new Item(
                item.getProductNameSnapshot(),
                item.getSkuSnapshot(),
                item.getQuantity(),
                item.getUnitFinalPriceSnapshot(),
                item.getLineTotal(),
                components
            );
        }

        public Item {
            components = List.copyOf(components);
        }
    }

    public record Component(
        String name,
        int quantity,
        ComponentRole role
    ) {

        /** Entity -> DTO */
        public static Component entityToDto(OrderItemComponent component) {
            return new Component(
                component.getComponentNameSnapshot(),
                component.getQuantityPerSet(),
                component.getComponentRole()
            );
        }
    }

    public record PaymentLine(
        String methodLabel,
        String statusLabel,
        boolean paid,
        BigDecimal amount,
        String formattedPaidAt
    ) {
    }

    public record Actions(
        boolean canStartProcessing,
        boolean canShip,
        boolean canComplete,
        boolean canCancel,
        boolean canCheckPayment,
        boolean needsRefund,
        boolean paidOnline
    ) {
    }
}
