package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.ProductVideo;

public record ProductVideoView(
    long id,
    String url,
    String caption
) {

    public static ProductVideoView entityToDto(ProductVideo video) {
        return new ProductVideoView(
            video.getId(),
            video.getVideoUrl(),
            video.getCaption()
        );
    }
}
