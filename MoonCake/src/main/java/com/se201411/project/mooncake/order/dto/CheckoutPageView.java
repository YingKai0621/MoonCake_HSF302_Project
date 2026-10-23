package com.se201411.project.mooncake.order.dto;

import java.util.List;

public record CheckoutPageView(
    long cartId,
    List<CheckoutLineView> lines,
    int totalQuantity,
    String formattedSubtotal,
    String formattedDiscount,
    String formattedShippingFee,
    String formattedRoundingAdjustment,
    String formattedTotal
) {
    public CheckoutPageView {
        lines = List.copyOf(lines);
    }
}
