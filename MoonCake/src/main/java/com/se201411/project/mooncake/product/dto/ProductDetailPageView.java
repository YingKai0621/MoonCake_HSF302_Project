package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record ProductDetailPageView(
    CatalogProductView product,
    String categoryUrl,
    String categoryName,
    List<CatalogProductView> relatedProducts
) {
    public ProductDetailPageView {
        relatedProducts = List.copyOf(relatedProducts);
    }
}
