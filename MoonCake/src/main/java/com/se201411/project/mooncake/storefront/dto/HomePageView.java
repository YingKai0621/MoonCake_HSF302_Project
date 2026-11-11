package com.se201411.project.mooncake.storefront.dto;

import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.feedback.dto.PublicReviewView;

import java.util.List;

public record HomePageView(
    List<PublicReviewView> reviews,
    List<CatalogProductView> giftBoxes
) {
    public HomePageView {
        reviews = List.copyOf(reviews);
        giftBoxes = List.copyOf(giftBoxes);
    }
}
