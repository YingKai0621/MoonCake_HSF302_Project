package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record NewProductPageView(ProductFormOptions formOptions, List<AdminPresetOptions.Option> boxes) {
    public NewProductPageView {
        boxes = List.copyOf(boxes);
    }
}
