package com.se201411.project.mooncake.product.dto;

import java.util.Map;
import java.util.List;

public record ProductDetailPageView(
    CatalogProductView product,
    String categoryUrl,
    String categoryName,
    List<CatalogProductView> relatedProducts,
    List<ProductImageView> galleryImages,
    List<ProductVideoView> galleryVideos,
    Map<Long, String> currentBatches
) {
    public ProductDetailPageView {
        relatedProducts = List.copyOf(relatedProducts);
        galleryImages = List.copyOf(galleryImages);
        galleryVideos = List.copyOf(galleryVideos);
        currentBatches = Map.copyOf(currentBatches);
    }

    public String currentBatch(long variantId) {
        return currentBatches.get(variantId);
    }
}
