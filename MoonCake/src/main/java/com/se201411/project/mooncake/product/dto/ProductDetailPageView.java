package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record ProductDetailPageView(
    CatalogProductView product,
    String categoryUrl,
    String categoryName,
    List<CatalogProductView> relatedProducts,
    List<ProductImageView> galleryImages,
    List<ProductVideoView> galleryVideos
) {
    public ProductDetailPageView {
        relatedProducts = List.copyOf(relatedProducts);
        galleryImages = List.copyOf(galleryImages);
        galleryVideos = List.copyOf(galleryVideos);
    }
}
