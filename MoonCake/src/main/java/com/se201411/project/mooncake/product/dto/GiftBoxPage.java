package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record GiftBoxPage(
    AdminProductDetail detail,
    AdminProductDetail.Sku sku,
    AdminPresetView recipe,
    List<Step> steps,
    String currentPanel,
    boolean onSale,
    boolean canUnlock,
    ProductFormOptions formOptions,
    AdminPresetOptions options
) {
    public record Step(int number, String label, String panel, boolean done) {
    }
}
