package com.se201411.project.mooncake.dashboard.dto;

import java.util.List;

public record DiscountSummaryView(
    String periodLabel,
    long orderCount,
    long totalDiscount,
    List<Order> recentOrders
) {
    public record Order(
        long id,
        String orderCode,
        String recipientName,
        String formattedDiscount,
        String formattedTotal,
        String formattedCreatedAt
    ) {
    }
}
