package com.se201411.project.mooncake.order.dto;

/**
 * Result of placing an order.
 *
 * @param orderCode  the new order's code
 * @param paymentUrl where to send the customer to pay online, or null when
 *                   no redirect is needed (COD)
 */
public record CheckoutResult(String orderCode, String paymentUrl) {

    public CheckoutResult(String orderCode) {
        this(orderCode, null);
    }

    public boolean needsPaymentRedirect() {
        return paymentUrl != null;
    }
}
