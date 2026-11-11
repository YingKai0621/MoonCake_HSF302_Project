package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.OrderItem;
import java.util.List;

public record CustomerOrderItemView(
    String productName,
    String sku,
    int quantity,
    String discountLabel,
    String formattedUnitFinalPrice,
    String formattedLineTotal,
    List<CustomerOrderComponentView> components
) {

    public static CustomerOrderItemView entityToDto(OrderItem item, List<CustomerOrderComponentView> components) {
        String sku = item.getSkuSnapshot();
        if (sku == null) {
            sku = item.getBoxSkuSnapshot();
        }
        return new CustomerOrderItemView(
            item.getProductNameSnapshot(),
            sku,
            item.getQuantity(),
            MoneyFormat.discountLabel(item.getDiscountPercentSnapshot()),
            MoneyFormat.vnd(item.getUnitFinalPriceSnapshot()),
            MoneyFormat.vnd(item.getLineTotal()),
            components
        );
    }

    public CustomerOrderItemView {
        components = List.copyOf(components);
    }
}
