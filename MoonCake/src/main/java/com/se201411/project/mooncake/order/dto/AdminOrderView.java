package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.ComponentRole;
import com.se201411.project.mooncake.enums.OrderStatus;

import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.common.format.VietnamTime;
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
    Actions actions,
    OrderFulfilmentView fulfilment,
    String autoCompleteDate,
    List<ShippingCarrier> shippingCarriers
) {

    public static AdminOrderView entityToDto(
        SalesOrder order,
        String statusLabel,
        String statusTone,
        String formattedCreatedAt,
        String formattedUpdatedAt,
        List<Item> items,
        List<PaymentLine> payments,
        Actions actions
    ) {
        return new AdminOrderView(
            order.getId(),
            order.getOrderCode(),
            order.getStatus(),
            statusLabel,
            statusTone,
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            order.getSubtotalAmount(),
            order.getDiscountAmount(),
            order.getShippingFee(),
            order.getRoundingAdjustment(),
            order.getTotalAmount(),
            formattedCreatedAt,
            formattedUpdatedAt,
            items,
            payments,
            actions,
            OrderFulfilmentView.entityToDto(order),
            autoCompleteDate(order),
            List.of(ShippingCarrier.values())
        );
    }

    private static String autoCompleteDate(SalesOrder order) {
        if (order.getDeliveredAt() == null) {
            return null;
        }
        return VietnamTime.date(order.getDeliveredAt().plusDays(3));
    }

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

        public static PaymentLine entityToDto(Payment payment, String methodLabel, String formattedPaidAt) {
            boolean paid = payment.getStatus() == PaymentStatus.PAID;
            String statusLabel = "Chưa thanh toán";
            if (paid) {
                statusLabel = "Đã thanh toán";
            }
            return new PaymentLine(
                methodLabel,
                statusLabel,
                paid,
                payment.getAmount(),
                formattedPaidAt
            );
        }
    }

    public record Actions(
        boolean canStartProcessing,
        boolean canShip,
        boolean canMarkDelivered,
        boolean canCancel,
        boolean canCheckPayment,
        boolean needsRefund,
        boolean paidOnline
    ) {
    }
}
