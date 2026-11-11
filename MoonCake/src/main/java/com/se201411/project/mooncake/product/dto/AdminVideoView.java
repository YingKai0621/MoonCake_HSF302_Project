package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.ProductVideo;

public record AdminVideoView(
    long id,
    String productName,
    String url,
    String caption
) {

    public static AdminVideoView entityToDto(ProductVideo video) {
        return new AdminVideoView(
            video.getId(),
            video.getProduct().getName(),
            video.getVideoUrl(),
            video.getCaption()
        );
    }
}
