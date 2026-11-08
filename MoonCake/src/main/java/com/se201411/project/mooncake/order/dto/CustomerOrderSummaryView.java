package com.se201411.project.mooncake.order.dto;

public record CustomerOrderSummaryView(
    String orderCode,
    String statusLabel,
    boolean awaitingPayment,
    String formattedCreatedAt,
    String formattedTotal
) {
}
