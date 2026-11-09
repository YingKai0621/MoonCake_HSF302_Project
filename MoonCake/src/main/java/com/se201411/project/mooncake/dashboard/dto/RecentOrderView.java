package com.se201411.project.mooncake.dashboard.dto;

public record RecentOrderView(
    long id,
    String orderCode,
    String recipientName,
    String formattedTotal,
    String statusLabel,
    String statusTone,
    String formattedCreatedAt
) {
}
