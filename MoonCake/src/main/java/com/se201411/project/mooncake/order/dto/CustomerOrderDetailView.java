package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.SalesOrder;
import java.util.List;

public record CustomerOrderDetailView(
    String orderCode,
    String statusLabel,
    boolean cancellable,
    boolean deliveryEditable,
    String formattedCreatedAt,
    String recipientName,
    String recipientPhone,
    String shippingAddress,
    String orderNote,
    String paymentStatusLabel,
    String paymentMethodLabel,
    boolean awaitingOnlinePayment,
    AutoCancelView autoCancel,
    List<CustomerOrderItemView> items,
    int totalQuantity,
    String formattedSubtotal,
    String formattedDiscount,
    String formattedShippingFee,
    String formattedRoundingAdjustment,
    String formattedTotal
) {

    public static CustomerOrderDetailView entityToDto(
        SalesOrder order,
        String statusLabel,
        boolean cancellable,
        boolean deliveryEditable,
        String formattedCreatedAt,
        String paymentStatusLabel,
        String paymentMethodLabel,
        boolean awaitingOnlinePayment,
        AutoCancelView autoCancel,
        List<CustomerOrderItemView> items,
        int totalQuantity
    ) {
        return new CustomerOrderDetailView(
            order.getOrderCode(),
            statusLabel,
            cancellable,
            deliveryEditable,
            formattedCreatedAt,
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            paymentStatusLabel,
            paymentMethodLabel,
            awaitingOnlinePayment,
            autoCancel,
            items,
            totalQuantity,
            MoneyFormat.vnd(order.getSubtotalAmount()),
            MoneyFormat.vnd(order.getDiscountAmount()),
            MoneyFormat.vnd(order.getShippingFee()),
            MoneyFormat.signedVnd(order.getRoundingAdjustment()),
            MoneyFormat.vnd(order.getTotalAmount())
        );
    }

    public CustomerOrderDetailView {
        items = List.copyOf(items);
    }
}
