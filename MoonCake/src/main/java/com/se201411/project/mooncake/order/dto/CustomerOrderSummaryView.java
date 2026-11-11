package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.SalesOrder;

public record CustomerOrderSummaryView(
    String orderCode,
    String statusLabel,
    boolean awaitingPayment,
    String formattedCreatedAt,
    String formattedTotal
) {

    /** Entity -> DTO. The service decides the status text and formats the time. */
    public static CustomerOrderSummaryView entityToDto(
        SalesOrder order,
        String statusLabel,
        boolean awaitingPayment,
        String formattedCreatedAt
    ) {
        return new CustomerOrderSummaryView(
            order.getOrderCode(),
            statusLabel,
            awaitingPayment,
            formattedCreatedAt,
            MoneyFormat.vnd(order.getTotalAmount())
        );
    }
}
