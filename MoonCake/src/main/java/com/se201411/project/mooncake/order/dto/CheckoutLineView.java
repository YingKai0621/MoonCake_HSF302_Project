package com.se201411.project.mooncake.order.dto;

public record CheckoutLineView(
    String productName,
    String sku,
    int quantity,
    String formattedUnitBasePrice,
    String discountLabel,
    String formattedLineTotal
) {
}
