package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.MediaPageView;
import com.se201411.project.mooncake.product.dto.AdminVideoView;
import com.se201411.project.mooncake.product.dto.ProductImageView;
import com.se201411.project.mooncake.product.dto.ProductVideoView;

import java.util.List;

public interface ProductMediaService {

    List<ProductImageView> images(long productId);

    List<ProductVideoView> videos(long productId);

    List<AdminVideoView> allVideos();

    void addVideo(long productId, String url, String caption);

    void removeVideo(long videoId);

    MediaPageView mediaPage();
}
