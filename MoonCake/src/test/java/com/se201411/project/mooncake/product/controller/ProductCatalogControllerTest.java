package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogPageView;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.CatalogVariantView;
import com.se201411.project.mooncake.product.dto.ProductDetailPageView;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ProductCatalogControllerTest {

    @Mock
    private ProductQueryService productQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ProductCatalogController(productQueryService))
            .build();
    }

    @Test
    void rendersMooncakeCatalogFromProductService() throws Exception {
        CatalogPageView page = new CatalogPageView(
            "mooncakes",
            "Bánh Trung Thu",
            "Mô tả",
            List.of(product()),
            List.of(),
            List.of("Lava trứng muối"),
            List.of(150)
        );
        when(productQueryService.mooncakeCatalogPage()).thenReturn(page);

        mockMvc.perform(get("/products/mooncakes"))
            .andExpect(status().isOk())
            .andExpect(view().name("shop-catalog"))
            .andExpect(model().attribute("page", page));

        assertThat(page.totalCount()).isEqualTo(1);
    }

    @Test
    void rendersProductDetailAndRelatedProducts() throws Exception {
        CatalogProductView product = product();
        ProductDetailPageView page = new ProductDetailPageView(product, "/products/mooncakes", "Bánh Trung Thu", List.of());
        when(productQueryService.productDetailPage(product.getSlug())).thenReturn(page);

        mockMvc.perform(get("/products/{slug}", product.getSlug()))
            .andExpect(status().isOk())
            .andExpect(view().name("product-detail"))
            .andExpect(model().attribute("page", page));

        assertThat(product.getSearchText())
            .contains("Bánh Lava", "CAKE-LAVA-150", "Lava trứng muối");
    }

    @Test
    void returnsNotFoundForUnknownProductSlug() throws Exception {
        when(productQueryService.productDetailPage("khong-ton-tai"))
            .thenThrow(new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        mockMvc.perform(get("/products/khong-ton-tai"))
            .andExpect(status().isNotFound());
    }

    @Test
    void keepsLegacyDemoUrlAsCatalogRedirect() throws Exception {
        mockMvc.perform(get("/products/demo"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products/gift-boxes"));
    }

    private CatalogProductView product() {
        CatalogVariantView variant = new CatalogVariantView(
            11L,
            "CAKE-LAVA-150",
            "150g",
            150,
            "Lava trứng muối",
            BigDecimal.valueOf(135_000),
            "135.000đ",
            "/images/branding/mooncake_luxury_emblem_v4.png",
            "Bánh Lava",
            null
        );
        return new CatalogProductView(
            1L,
            "banh-lava",
            ProductType.MOONCAKE,
            "Bánh Lava",
            "Bánh 150g",
            null,
            null,
            null,
            null,
            List.of(variant)
        );
    }
}
