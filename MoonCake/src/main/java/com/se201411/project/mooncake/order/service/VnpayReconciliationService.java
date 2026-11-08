package com.se201411.project.mooncake.order.service;

/**
 * Asks VNPay (querydr) for the real result of a pending attempt when the IPN
 * did not arrive, for example when the app runs on localhost. An attempt that
 * expired and was never paid is closed, so the order can be cancelled or paid
 * again (Business Rules v1.7, 6.3).
 */
public interface VnpayReconciliationService {

    /** Customer checks one of their own orders. Returns a message to show. */
    String checkPayment(String orderCode, String clientIp);

    /** Admin checks any order. Returns a message to show. */
    String checkPaymentForAdmin(long orderId, String clientIp);
}
