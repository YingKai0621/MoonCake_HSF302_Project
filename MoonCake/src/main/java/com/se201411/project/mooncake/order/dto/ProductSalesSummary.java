package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;

public record ProductSalesSummary(
    String name,
    Long quantity,
    BigDecimal revenue
) {
}
