package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.common.format.VietnamTime;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import java.util.List;

public record CustomerOrderDetailView(
    String orderCode,
    String statusLabel,
    boolean cancellable,
    boolean deliveryEditable,
    boolean receivable,
    String autoCompleteDate,
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
    String formattedTotal,
    String cancellationNote,
    OrderFulfilmentView fulfilment
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
        int totalQuantity,
        String cancellationNote
    ) {
        return new CustomerOrderDetailView(
            order.getOrderCode(),
            statusLabel,
            cancellable,
            deliveryEditable,
            order.getStatus() == OrderStatus.DELIVERED,
            autoCompleteDate(order),
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
            MoneyFormat.vnd(order.getTotalAmount()),
            cancellationNote,
            OrderFulfilmentView.entityToDto(order)
        );
    }

    private static String autoCompleteDate(SalesOrder order) {
        if (order.getDeliveredAt() == null) {
            return null;
        }
        return VietnamTime.date(order.getDeliveredAt().plusDays(3));
    }

    public CustomerOrderDetailView {
        items = List.copyOf(items);
    }
}
