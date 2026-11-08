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

    /**
     * Returns the pending attempt of any order, or null. Callers must have
     * authorized the request already (used for Admin reconciliation).
     */
    VnpayPendingAttempt findPendingAttemptForOrder(long orderId);

    /**
     * Applies a verified querydr (reconciliation) result. An attempt whose
     * payment window has passed and that VNPay reports as not found or not
     * completed is closed as FAILED.
     */
    Map<String, String> applyQueryResult(Map<String, String> params);

    /**
     * Throws a business exception while a VNPay attempt of the order is still
     * open: either inside its 15-minute window, or expired but not yet
     * reconciled with VNPay.
     */
    void requireNoOpenAttempt(long orderId);
}
