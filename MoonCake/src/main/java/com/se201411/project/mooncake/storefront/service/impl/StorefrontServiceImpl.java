package com.se201411.project.mooncake.storefront.service.impl;

import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.storefront.dto.HomePageView;
import com.se201411.project.mooncake.storefront.service.StorefrontService;
import org.springframework.stereotype.Service;

@Service
public class StorefrontServiceImpl implements StorefrontService {

    private final FeedbackService feedbackService;

    public StorefrontServiceImpl(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Override
    public HomePageView homePage() {
        return new HomePageView(feedbackService.findPublishedReviews());
    }
}
