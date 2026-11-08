package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A VNPay payment attempt that is still waiting for VNPay's final result.
 * Times are UTC.
 */
public record VnpayPendingAttempt(
    String transactionRef,
    BigDecimal amount,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {

    /** True once the 15-minute payment window has passed. */
    public boolean isExpiredAt(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }
}
