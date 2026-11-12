package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.entity.SalesOrder;

public record CheckoutResult(String orderCode, String paymentUrl) {
    public CheckoutResult(String orderCode) {
        this(orderCode, null);
    }

    public static CheckoutResult entityToDto(SalesOrder order, String paymentUrl) {
        return new CheckoutResult(order.getOrderCode(), paymentUrl);
    }

    public boolean needsPaymentRedirect() {
        return paymentUrl != null;
    }
}
