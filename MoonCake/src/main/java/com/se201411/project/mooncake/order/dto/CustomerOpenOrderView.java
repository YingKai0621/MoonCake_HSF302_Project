package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;

public record CustomerOpenOrderView(
    long orderId,
    String orderCode,
    String statusLabel,
    String paymentLabel,
    String formattedTotal,
    boolean cancellableOnLock
) {

    public static CustomerOpenOrderView entityToDto(
        SalesOrder order,
        Payment payment,
        String statusLabel,
        String paymentMethodLabel
    ) {
        boolean paid = payment.getStatus() == PaymentStatus.PAID;
        String paymentStatusLabel = "Chưa thanh toán";
        if (paid) {
            paymentStatusLabel = "Đã thanh toán";
        }
        boolean placed = order.getStatus() == OrderStatus.PLACED;
        boolean unpaid = payment.getStatus() == PaymentStatus.UNPAID;
        return new CustomerOpenOrderView(
            order.getId(),
            order.getOrderCode(),
            statusLabel,
            paymentMethodLabel + " · " + paymentStatusLabel,
            MoneyFormat.vnd(order.getTotalAmount()),
            placed && unpaid
        );
    }
}
