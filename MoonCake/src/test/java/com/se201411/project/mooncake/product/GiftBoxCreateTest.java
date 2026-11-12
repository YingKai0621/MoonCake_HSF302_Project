package com.se201411.project.mooncake.product;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.NewProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;
import com.se201411.project.mooncake.product.dto.AdminPresetForm;
import com.se201411.project.mooncake.product.dto.AdminProductDetail;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.product.service.impl.GiftBoxAdminServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GiftBoxCreateTest {
    private final ProductAdminService products = mock(ProductAdminService.class);
    private final PresetConfigurationAdminService recipes = mock(PresetConfigurationAdminService.class);
    private final GiftBoxAdminServiceImpl service = new GiftBoxAdminServiceImpl(products, recipes);

    @Test
    void giftBoxWithoutBoxIsRejectedBeforeAnythingIsSaved() {
        assertThatThrownBy(() -> service.create(giftBoxForm(false), null))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("chọn vỏ hộp");
        verify(products, never()).createProduct(any());
    }

    @Test
    void giftBoxIsCreatedWithADraftRecipeOnTheChosenBox() {
        when(products.createProduct(any())).thenReturn(9L);
        when(products.productDetail(9L)).thenReturn(detailWithSku(77L));

        long productId = service.create(giftBoxForm(false), 3L);

        assertThat(productId).isEqualTo(9L);
        verify(recipes).saveDraft(new AdminPresetForm(null, 77L, 3L));
        verify(products, never()).setProductVisible(9L, false);
    }

    @Test
    void newGiftBoxStaysHiddenUntilItIsPutOnSale() {
        when(products.createProduct(any())).thenReturn(9L);
        when(products.productDetail(9L)).thenReturn(detailWithSku(77L));

        service.create(giftBoxForm(true), 3L);

        verify(products).setProductVisible(9L, false);
    }

    private NewProductForm giftBoxForm(boolean active) {
        return new NewProductForm(
            1,
            ProductType.GIFT_SET,
            "Hộp quà mới",
            "hop-qua-moi",
            "Mô tả",
            null,
            null,
            30,
            null,
            active,
            "GIFT-NEW",
            "Hộp quà",
            BigDecimal.valueOf(450_000),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private AdminProductDetail detailWithSku(long variantId) {
        VariantForm variant = new VariantForm(
            variantId,
            9,
            "GIFT-NEW",
            "Hộp quà",
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(450_000),
            false
        );
        AdminProductDetail.Sku sku = new AdminProductDetail.Sku(variant, null, List.of(), 0, "Ngừng bán", "muted", null, null);
        ProductForm product = new ProductForm(9L, 1, ProductType.GIFT_SET, "Hộp quà mới", "hop-qua-moi", "Mô tả", null, null, 30, null, false);
        return new AdminProductDetail(product, "Hộp quà", true, false, "Chưa mở bán", "warning", null, List.of(sku));
    }
}
