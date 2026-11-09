package com.se201411.project.mooncake.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaidAmount(
    BigDecimal amount,
    LocalDateTime paidAt
) {
}
