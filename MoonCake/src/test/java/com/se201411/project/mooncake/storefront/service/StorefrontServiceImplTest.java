package com.se201411.project.mooncake.storefront.service;

import com.se201411.project.mooncake.feedback.dto.PublicReviewView;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.storefront.dto.HomePageView;
import com.se201411.project.mooncake.storefront.service.impl.StorefrontServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorefrontServiceImplTest {

    private final FeedbackService feedbackService = mock(FeedbackService.class);
    private final ProductQueryService productQueryService = mock(ProductQueryService.class);
    private final StorefrontServiceImpl service = new StorefrontServiceImpl(feedbackService, productQueryService);

    @Test
    void homePageCarriesPublishedReviews() {
        List<PublicReviewView> reviews = List.of(new PublicReviewView("An", "A", 5, "Bánh ngon"));
        when(feedbackService.findPublishedReviews()).thenReturn(reviews);

        HomePageView page = service.homePage();

        assertThat(page.reviews()).isEqualTo(reviews);
    }

    @Test
    void homePageListsEveryActiveGiftBoxFromTheCatalog() {
        when(productQueryService.findActiveCatalog(ProductType.GIFT_SET)).thenReturn(List.of());

        service.homePage();

        verify(productQueryService).findActiveCatalog(ProductType.GIFT_SET);
    }
}
