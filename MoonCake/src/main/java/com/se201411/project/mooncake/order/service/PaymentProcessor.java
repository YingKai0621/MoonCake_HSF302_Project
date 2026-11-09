package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.dto.PaymentReturnResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface PaymentProcessor {
    PaymentMethod method();

    String displayName();

    boolean isOnline();

    boolean isAvailable();

    Optional<String> startPayment(SalesOrder order, String clientIp);

    void checkCanCancel(long orderId, Payment payment);

    void checkCanFulfil(Payment payment);

    void completeDelivery(long orderId, Payment payment, LocalDateTime now);

    void refreshPaymentStatus(long orderId);

    Optional<LocalDateTime> autoCancelAt(SalesOrder order, Payment payment);

    Map<String, String> handleNotification(Map<String, String> params);

    PaymentReturnResult handleReturn(Map<String, String> params);
}
