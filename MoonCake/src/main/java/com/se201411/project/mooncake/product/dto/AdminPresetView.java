package com.se201411.project.mooncake.product.dto;

import java.util.List;

import com.se201411.project.mooncake.enums.ConfigurationStatus;

public record AdminPresetView(long id, long giftSetVariantId, String sku, long boxId,
                              int capacity, ConfigurationStatus status, List<Component> components) {
    public AdminPresetView {
        components = List.copyOf(components);
    }

    public record Component(long variantId, String sku, int quantity) {
    }
}
