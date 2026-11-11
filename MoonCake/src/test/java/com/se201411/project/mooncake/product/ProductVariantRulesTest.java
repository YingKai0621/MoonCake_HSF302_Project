package com.se201411.project.mooncake.product;

import java.util.List;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.cart.service.CustomBoxService;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;
import com.se201411.project.mooncake.product.repository.PresetConfigurationRepository;
import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.product.service.ProductImageStorageService;
import com.se201411.project.mooncake.product.service.impl.ProductAdminServiceImpl;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductVariantRulesTest {
    private final ProductAdminRepository repo = mock(ProductAdminRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final CartService carts = mock(CartService.class);
    private ProductAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductAdminServiceImpl(
            repo,
            inventory,
            mock(PresetConfigurationQueryService.class),
            mock(AdminAccessService.class),
            mock(AdminFormValidation.class),
            mock(ProductImageStorageService.class),
            mock(InventoryAdminService.class),
            mock(PresetConfigurationRepository.class),
            mock(OrderManagementService.class),
            carts,
            mock(CustomBoxService.class)
        );
        Product cake = new Product();
        ReflectionTestUtils.setField(cake, "id", 1L);
        cake.setProductType(ProductType.MOONCAKE);
        when(repo.product(1)).thenReturn(cake);
    }

    @Test
    void cakeWithoutWeightIsRejected() {
        VariantForm form = new VariantForm(null, 1, "MC-NEW", "Bánh 150g", null, "Thập cẩm", BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("khối lượng");
        verify(repo, never()).save(any(com.se201411.project.mooncake.entity.ProductVariant.class));
    }

    @Test
    void cakeWithoutFillingIsRejected() {
        VariantForm form = new VariantForm(null, 1, "MC-NEW", "Bánh 150g", 150, " ", BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("nhân bánh");
    }

    @Test
    void priceMustBeWholeThousands() {
        VariantForm form = new VariantForm(null, 1, "MC-NEW", "Bánh 150g", 150, "Thập cẩm", BigDecimal.valueOf(65_500), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("bội số của 1.000đ");
    }

    @Test
    void duplicateSkuIsRejectedWithClearMessage() {
        when(repo.skuExists("MC-KSPM-080")).thenReturn(true);
        VariantForm form = new VariantForm(null, 1, "mc-kspm-080", "Bánh 80g", 80, "Kim sa", BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đã tồn tại");
    }

    @Test
    void skuWithSpacesOrAccentsIsRejected() {
        VariantForm form = new VariantForm(null, 1, "bánh mới", "Bánh 150g", 150, "Thập cẩm", BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Mã SKU chỉ dùng");
    }

    @Test
    void variantInCustomerCartCannotBeDeleted() {
        ProductVariant variant = cakeVariant(5L);
        when(repo.variant(5L)).thenReturn(variant);
        when(carts.countItemsWithVariants(List.of(5L))).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteVariant(5L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("giỏ hàng");
        verify(repo, never()).deleteVariant(5L);
        verify(inventory, never()).deleteBalances(List.of(5L));
    }

    @Test
    void unusedVariantIsDeletedWithItsEmptyBalance() {
        ProductVariant variant = cakeVariant(5L);
        when(repo.variant(5L)).thenReturn(variant);

        service.deleteVariant(5L);

        verify(inventory).deleteBalances(List.of(5L));
        verify(repo).deleteVariant(5L);
    }

    private ProductVariant cakeVariant(long id) {
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        variant.setProduct(repo.product(1));
        return variant;
    }
}
