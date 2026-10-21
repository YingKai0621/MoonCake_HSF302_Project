package com.se201411.project.mooncake.order.dto;

public record CustomerOrderComponentView(
    String roleLabel,
    String name,
    int quantityPerSet
) {
}
