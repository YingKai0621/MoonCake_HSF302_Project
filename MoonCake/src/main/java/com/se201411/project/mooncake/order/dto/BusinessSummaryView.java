package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;

public record BusinessSummaryView(
    Long id,
    String name,
    String contactName,
    String contactEmail,
    String contactPhone,
    Long orderCount,
    BigDecimal totalOrderValue
) {
}
