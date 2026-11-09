package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;

public record OverviewStatsView(
    Long totalOrders,
    Long placedOrders,
    BigDecimal completedRevenue,
    Long cancelledOrders
) {
}
