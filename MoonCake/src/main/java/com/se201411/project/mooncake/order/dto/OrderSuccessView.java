package com.se201411.project.mooncake.order.dto;

import java.util.List;

public record OrderSuccessView(
    String orderCode,
    String recipientName,
    String recipientPhone,
    String shippingAddress,
    String orderNote,
    List<OrderSuccessItemView> items,
    int totalQuantity,
    String formattedSubtotal,
    String formattedDiscount,
    String formattedShippingFee,
    String formattedTotal
) {
    public OrderSuccessView {
        items = List.copyOf(items);
    }
}
