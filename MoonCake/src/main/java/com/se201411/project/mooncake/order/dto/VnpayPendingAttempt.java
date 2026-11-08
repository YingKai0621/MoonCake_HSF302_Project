package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A VNPay payment attempt that is still waiting for VNPay's final result.
 */
public record VnpayPendingAttempt(
    String transactionRef,
    BigDecimal amount,
    LocalDateTime createdAt
) {
}
