package com.se201411.project.mooncake.order.service;

/**
 * Asks VNPay (querydr) for the real result of a pending attempt when the IPN
 * did not arrive, for example when the app runs on localhost.
 */
public interface VnpayReconciliationService {

    /** Returns a message for the customer describing the outcome. */
    String checkPayment(String orderCode, String clientIp);
}
