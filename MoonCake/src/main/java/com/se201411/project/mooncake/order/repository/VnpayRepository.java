package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.SalesOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VnpayRepository {
    record Attempt(
        String transactionRef,
        long orderId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
    ) {
    }

    SalesOrder lockOrder(long orderId);

    Attempt findAttempt(String transactionRef);

    Attempt findPendingAttempt(long orderId);

    void createAttempt(String transactionRef, long orderId, BigDecimal amount, LocalDateTime createdAt);

    void markFailed(String transactionRef);

    void markPaid(String transactionRef, String transactionNo);
}
