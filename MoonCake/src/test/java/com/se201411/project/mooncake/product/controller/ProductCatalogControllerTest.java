package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.CatalogVariantView;
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
        CatalogProductView product = product();
        when(productQueryService.findActiveCatalog(ProductType.MOONCAKE))
            .thenReturn(List.of(product));

        mockMvc.perform(get("/products/mooncakes"))
            .andExpect(status().isOk())
            .andExpect(view().name("shop-catalog"))
            .andExpect(model().attribute("products", List.of(product)))
            .andExpect(model().attribute("totalCount", 1))
            .andExpect(model().attribute("fillingOptions", List.of("Lava trứng muối")))
            .andExpect(model().attribute("weightOptions", List.of(150)));
    }

    @Test
    void rendersProductDetailAndRelatedProducts() throws Exception {
        CatalogProductView product = product();
        when(productQueryService.requireActiveProduct(product.getSlug())).thenReturn(product);
        when(productQueryService.findActiveCatalog(ProductType.MOONCAKE))
            .thenReturn(List.of(product));

        mockMvc.perform(get("/products/{slug}", product.getSlug()))
            .andExpect(status().isOk())
            .andExpect(view().name("product-detail"))
            .andExpect(model().attribute("product", product))
            .andExpect(model().attribute("relatedProducts", List.of()));

        assertThat(product.getSearchText())
            .contains("Bánh Lava", "CAKE-LAVA-150", "Lava trứng muối");
    }

    @Test
    void returnsNotFoundForUnknownProductSlug() throws Exception {
        when(productQueryService.requireActiveProduct("khong-ton-tai"))
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
