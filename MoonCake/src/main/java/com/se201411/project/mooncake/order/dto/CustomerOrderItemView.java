package com.se201411.project.mooncake.order.dto;

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
    public CustomerOrderItemView {
        components = List.copyOf(components);
    }
}
