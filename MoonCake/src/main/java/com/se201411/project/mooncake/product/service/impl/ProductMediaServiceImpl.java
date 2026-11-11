package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.product.dto.MediaPageView;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVideo;
import com.se201411.project.mooncake.product.dto.AdminVideoView;
import com.se201411.project.mooncake.product.dto.ProductImageView;
import com.se201411.project.mooncake.product.dto.ProductVideoView;
import com.se201411.project.mooncake.product.repository.ProductImageRepository;
import com.se201411.project.mooncake.product.repository.ProductRepository;
import com.se201411.project.mooncake.product.repository.ProductVideoRepository;
import com.se201411.project.mooncake.product.service.ProductMediaService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ProductMediaServiceImpl implements ProductMediaService {

    private static final Pattern VIDEO_URL =
        Pattern.compile("(?:https://[^\\s]+|/images/[^\\s]+)\\.(?:mp4|webm)(?:\\?[^\\s]*)?");
    private static final int MAX_URL_LENGTH = 1000;
    private static final int MAX_CAPTION_LENGTH = 250;

    private final ProductImageRepository productImageRepository;
    private final ProductVideoRepository productVideoRepository;
    private final ProductRepository productRepository;
    private final AdminAccessService adminAccessService;
    private final ProductAdminService productAdminService;

    public ProductMediaServiceImpl(
        ProductImageRepository productImageRepository,
        ProductVideoRepository productVideoRepository,
        ProductRepository productRepository,
        AdminAccessService adminAccessService,
        ProductAdminService productAdminService
    ) {
        this.productImageRepository = productImageRepository;
        this.productVideoRepository = productVideoRepository;
        this.productRepository = productRepository;
        this.adminAccessService = adminAccessService;
        this.productAdminService = productAdminService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageView> images(long productId) {
        List<ProductImageView> views = new ArrayList<>();
        for (ProductImage image : productImageRepository.findGalleryByProductId(productId)) {
            views.add(ProductImageView.entityToDto(image));
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVideoView> videos(long productId) {
        List<ProductVideoView> views = new ArrayList<>();
        for (ProductVideo video : productVideoRepository.findByProductIdOrderBySortOrderAscIdAsc(productId)) {
            views.add(ProductVideoView.entityToDto(video));
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminVideoView> allVideos() {
        adminAccessService.requireAdmin();
        List<AdminVideoView> views = new ArrayList<>();
        for (ProductVideo video : productVideoRepository.findAllByOrderByIdDesc()) {
            views.add(AdminVideoView.entityToDto(video));
        }
        return views;
    }

    @Override
    @Transactional
    public void addVideo(long productId, String url, String caption) {
        adminAccessService.requireAdmin();
        boolean validUrl = url != null
            && url.length() <= MAX_URL_LENGTH
            && VIDEO_URL.matcher(url).matches();
        boolean validCaption = caption != null
            && !caption.isBlank()
            && caption.length() <= MAX_CAPTION_LENGTH;
        if (!validUrl || !validCaption) {
            throw new BusinessRuleViolationException(
                "Nhập link HTTPS hoặc /images/ tới video MP4/WebM và mô tả tối đa 250 ký tự."
            );
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));

        ProductVideo video = new ProductVideo();
        video.setProduct(product);
        video.setVideoUrl(url);
        video.setCaption(caption.trim());
        video.setSortOrder(0);
        productVideoRepository.save(video);
    }

    @Override
    @Transactional
    public void removeVideo(long videoId) {
        adminAccessService.requireAdmin();
        productVideoRepository.deleteById(videoId);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaPageView mediaPage() {
        return new MediaPageView(allVideos(), productAdminService.catalog());
    }
}
