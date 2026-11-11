package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record MediaPageView(
    List<AdminVideoView> videos,
    AdminCatalogView catalog
) {
    public MediaPageView {
        videos = List.copyOf(videos);
    }
}
