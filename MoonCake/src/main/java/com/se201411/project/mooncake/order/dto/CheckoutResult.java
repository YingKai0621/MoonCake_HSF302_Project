package com.se201411.project.mooncake.order.dto;

public record CheckoutResult(String orderCode, String paymentUrl) {
    public CheckoutResult(String orderCode) {
        this(orderCode, null);
    }

    public boolean needsPaymentRedirect() {
        return paymentUrl != null;
    }
}
