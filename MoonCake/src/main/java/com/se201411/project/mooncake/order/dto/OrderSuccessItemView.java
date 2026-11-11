package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.OrderItem;

public record OrderSuccessItemView(
    String productName,
    String sku,
    int quantity,
    String formattedUnitPrice,
    String formattedLineTotal
) {

    public static OrderSuccessItemView entityToDto(OrderItem item) {
        String sku = item.getSkuSnapshot();
        if (sku == null) {
            sku = item.getBoxSkuSnapshot();
        }
        return new OrderSuccessItemView(
            item.getProductNameSnapshot(),
            sku,
            item.getQuantity(),
            MoneyFormat.vnd(item.getUnitFinalPriceSnapshot()),
            MoneyFormat.vnd(item.getLineTotal())
        );
    }
}
