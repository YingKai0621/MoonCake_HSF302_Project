package com.se201411.project.mooncake.storefront.controller;

import com.se201411.project.mooncake.feedback.dto.PublicReviewView;
import com.se201411.project.mooncake.storefront.dto.HomePageView;
import com.se201411.project.mooncake.storefront.service.StorefrontService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorefrontPageControllerTest {

    private final StorefrontService storefrontService = mock(StorefrontService.class);
    private final StorefrontPageController controller = new StorefrontPageController(storefrontService);

    @Test
    void rendersHomepageWithOnePageView() {
        HomePageView page = new HomePageView(List.of(new PublicReviewView("An", "A", 5, "Bánh ngon")), List.of());
        when(storefrontService.homePage()).thenReturn(page);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.homepage(model)).isEqualTo("storefront/homepage");
        assertThat(model.getAttribute("page")).isEqualTo(page);
        assertThat(model.size()).isEqualTo(1);
    }

    @Test
    void rendersBusinessPage() {
        assertThat(controller.business()).isEqualTo("storefront/business");
    }
}
