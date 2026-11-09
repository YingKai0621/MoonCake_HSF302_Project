package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.enums.OrderStatus;

public record AdminOrderTab(
    OrderStatus status,
    String label,
    long count,
    boolean selected
) {
}
