package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VnpayAttemptView(
    String transactionRef,
    long orderId,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt
) {
}
