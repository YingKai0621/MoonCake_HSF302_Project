package com.se201411.project.mooncake.cart.dto;

public record CustomCartComponentView(
    String name,
    String variantName,
    int quantityPerBox
) {
}
