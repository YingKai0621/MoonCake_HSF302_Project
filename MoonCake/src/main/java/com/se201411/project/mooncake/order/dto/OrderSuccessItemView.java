package com.se201411.project.mooncake.order.dto;

public record OrderSuccessItemView(
    String productName,
    String sku,
    int quantity,
    String formattedUnitPrice,
    String formattedLineTotal
) {
}
