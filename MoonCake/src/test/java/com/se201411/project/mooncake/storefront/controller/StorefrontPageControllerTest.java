package com.se201411.project.mooncake.storefront.controller;

import com.se201411.project.mooncake.feedback.dto.PublicReviewView;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorefrontPageControllerTest {

    private final FeedbackService feedbackService = mock(FeedbackService.class);
    private final StorefrontPageController controller = new StorefrontPageController(feedbackService);

    @Test
    void rendersHomepageWithPublishedReviews() {
        List<PublicReviewView> reviews = List.of(new PublicReviewView("An", "A", 5, "Bánh ngon"));
        when(feedbackService.findPublishedReviews()).thenReturn(reviews);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.homepage(model)).isEqualTo("homepage");
        assertThat(model.getAttribute("reviews")).isEqualTo(reviews);
    }

    @Test
    void rendersBusinessPage() {
        assertThat(controller.business()).isEqualTo("business");
    }
}
