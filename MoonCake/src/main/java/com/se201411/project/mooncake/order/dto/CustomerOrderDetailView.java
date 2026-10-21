package com.se201411.project.mooncake.order.dto;

import java.util.List;

public record CustomerOrderDetailView(
    String orderCode,
    String statusLabel,
    String formattedCreatedAt,
    String recipientName,
    String recipientPhone,
    String shippingAddress,
    String orderNote,
    String paymentStatusLabel,
    List<CustomerOrderItemView> items,
    int totalQuantity,
    String formattedSubtotal,
    String formattedDiscount,
    String formattedShippingFee,
    String formattedTotal
) {
    public CustomerOrderDetailView {
        items = List.copyOf(items);
    }
}
