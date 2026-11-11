package com.se201411.project.mooncake.dashboard.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.SalesOrder;
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

        /** Entity -> DTO. The service formats the time and passes it in. */
        public static Order entityToDto(SalesOrder order, String formattedCreatedAt) {
            return new Order(
                order.getId(),
                order.getOrderCode(),
                order.getRecipientName(),
                MoneyFormat.vnd(order.getDiscountAmount()),
                MoneyFormat.vnd(order.getTotalAmount()),
                formattedCreatedAt
            );
        }
    }
}
