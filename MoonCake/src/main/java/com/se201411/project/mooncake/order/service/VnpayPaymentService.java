package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.VnpayPendingAttempt;

import java.util.Map;

/**
 * VNPay Sandbox use cases. Controllers call this service; they never call the
 * VNPay gateway client directly.
 */
public interface VnpayPaymentService {

    /** True when VNPay is switched on in the configuration. */
    boolean isEnabled();

    /**
     * Opens (or reuses) a VNPay attempt for an order owned by the current
     * customer and returns the URL of the VNPay payment page.
     */
    String startPayment(String orderCode, String clientIp);

    /**
     * Handles the server-to-server IPN call from VNPay and returns the
     * response body VNPay expects (RspCode + Message).
     */
    Map<String, String> handleIpn(Map<String, String> params);

    /** Checks the signature of the browser Return URL. Never marks PAID. */
    boolean isValidReturn(Map<String, String> params);

    /** Returns the pending attempt of the current customer's order, or null. */
    VnpayPendingAttempt findPendingAttempt(String orderCode);

    /** Applies a verified querydr (reconciliation) result. */
    Map<String, String> applyQueryResult(Map<String, String> params);
}
