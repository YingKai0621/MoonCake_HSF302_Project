package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.entity.OrderItemComponent;

public record CustomerOrderComponentView(
    String roleLabel,
    String name,
    int quantityPerSet
) {

    /** Entity -> DTO. The service turns the component role into a Vietnamese label. */
    public static CustomerOrderComponentView entityToDto(OrderItemComponent component, String roleLabel) {
        return new CustomerOrderComponentView(
            roleLabel,
            component.getComponentNameSnapshot(),
            component.getQuantityPerSet()
        );
    }
}
