package com.se201411.project.mooncake.order.dto;

public record CustomerOrderSummaryView(
    String orderCode,
    String statusLabel,
    String formattedCreatedAt,
    String formattedTotal
) {
}
