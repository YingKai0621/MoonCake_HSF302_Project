package com.se201411.project.mooncake.dashboard.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.SalesOrder;

public record RecentOrderView(
    long id,
    String orderCode,
    String recipientName,
    String formattedTotal,
    String statusLabel,
    String statusTone,
    String formattedCreatedAt
) {

    public static RecentOrderView entityToDto(
        SalesOrder order,
        String statusLabel,
        String statusTone,
        String formattedCreatedAt
    ) {
        return new RecentOrderView(
            order.getId(),
            order.getOrderCode(),
            order.getRecipientName(),
            MoneyFormat.vnd(order.getTotalAmount()),
            statusLabel,
            statusTone,
            formattedCreatedAt
        );
    }
}
