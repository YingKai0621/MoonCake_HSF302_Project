package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record CatalogPageView(
    String category,
    String title,
    String description,
    List<CatalogProductView> products,
    List<Integer> boxCountOptions,
    List<String> fillingOptions,
    List<Integer> weightOptions
) {
    public CatalogPageView {
        products = List.copyOf(products);
        boxCountOptions = List.copyOf(boxCountOptions);
        fillingOptions = List.copyOf(fillingOptions);
        weightOptions = List.copyOf(weightOptions);
    }

    public int totalCount() {
        return products.size();
    }
}
