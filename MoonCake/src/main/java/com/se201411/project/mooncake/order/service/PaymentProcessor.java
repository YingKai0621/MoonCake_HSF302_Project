package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Strategy for one payment method (COD, VNPay, ...).
 *
 * Checkout, cancellation and order management only talk to this interface,
 * so they never need "if (method == VNPAY)". Supporting a new payment method
 * means adding one new implementation class.
 *
 * Every method runs inside the caller's transaction.
 */
public interface PaymentProcessor {

    /** The payment method this processor handles. */
    PaymentMethod method();

    /**
     * Called right after checkout has saved the order and its UNPAID payment.
     * Returns the URL the customer must be sent to, or empty when no redirect
     * is needed.
     */
    Optional<String> startPayment(SalesOrder order, String clientIp);

    /** Throws a business exception when this order may not be cancelled now. */
    void checkCanCancel(Payment payment);

    /**
     * Throws a business exception when the shop may not move the order
     * forward (process, ship, complete) yet.
     */
    void checkCanFulfil(Payment payment);

    /** Called in the same transaction as SHIPPING -> COMPLETED. */
    void completeDelivery(long orderId, Payment payment, LocalDateTime now);
}
