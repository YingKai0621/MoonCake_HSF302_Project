package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.entity.OrderItemComponent;

public record CustomerOrderComponentView(
    String roleLabel,
    String name,
    int quantityPerSet
) {

    public static CustomerOrderComponentView entityToDto(OrderItemComponent component, String roleLabel) {
        return new CustomerOrderComponentView(
            roleLabel,
            component.getComponentNameSnapshot(),
            component.getQuantityPerSet()
        );
    }
}
