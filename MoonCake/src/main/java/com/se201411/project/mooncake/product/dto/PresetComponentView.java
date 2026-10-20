package com.se201411.project.mooncake.product.dto;

import java.util.Objects;

public record PresetComponentView(
    long variantId,
    String sku,
    String name,
    int quantityPerBox
) {
    public PresetComponentView {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (quantityPerBox <= 0) {
            throw new IllegalArgumentException("quantityPerBox must be positive");
        }
    }
}
