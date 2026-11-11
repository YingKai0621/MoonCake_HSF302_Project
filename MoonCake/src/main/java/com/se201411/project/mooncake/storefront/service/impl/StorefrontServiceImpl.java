package com.se201411.project.mooncake.storefront.service.impl;

import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.storefront.dto.HomePageView;
import com.se201411.project.mooncake.storefront.service.StorefrontService;
import org.springframework.stereotype.Service;

@Service
public class StorefrontServiceImpl implements StorefrontService {

    private final FeedbackService feedbackService;
    private final ProductQueryService productQueryService;

    public StorefrontServiceImpl(FeedbackService feedbackService, ProductQueryService productQueryService) {
        this.feedbackService = feedbackService;
        this.productQueryService = productQueryService;
    }

    @Override
    public HomePageView homePage() {
        return new HomePageView(
            feedbackService.findPublishedReviews(),
            productQueryService.findActiveCatalog(ProductType.GIFT_SET)
        );
    }
}
