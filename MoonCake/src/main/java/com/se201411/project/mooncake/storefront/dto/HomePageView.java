package com.se201411.project.mooncake.storefront.dto;

import com.se201411.project.mooncake.feedback.dto.PublicReviewView;

import java.util.List;

public record HomePageView(List<PublicReviewView> reviews) {
    public HomePageView {
        reviews = List.copyOf(reviews);
    }
}
