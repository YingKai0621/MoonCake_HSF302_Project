package com.se201411.project.mooncake.product;

import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import java.util.List;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.cart.service.CustomBoxService;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.Box;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
        Product emptyBox = new Product();
        ReflectionTestUtils.setField(emptyBox, "id", 2L);
        emptyBox.setProductType(ProductType.EMPTY_BOX);
        when(repo.product(2)).thenReturn(emptyBox);
        doAnswer(call -> {
            ProductVariant saved = call.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", 50L);
            }
            return null;
        }).when(repo).save(any(ProductVariant.class));
    }

    @Test
    void emptyBoxVariantWithoutCapacityIsRejected() {
        VariantForm form = new VariantForm(null, 2, "BOX-NEW", "Hộp 4 bánh", null, null, null, null, BigDecimal.valueOf(30_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("sức chứa");
        verify(repo, never()).save(any(ProductVariant.class));
    }

    @Test
    void emptyBoxVariantSavesItsCapacityInTheSameStep() {
        VariantForm form = new VariantForm(null, 2, "BOX-NEW", "Hộp 4 bánh", null, null, 4, "Giấy kraft", BigDecimal.valueOf(30_000), false);

        service.saveVariant(form);

        ArgumentCaptor<Box> box = ArgumentCaptor.forClass(Box.class);
        verify(repo).save(box.capture());
        assertThat(box.getValue().getCapacityCakes()).isEqualTo(4);
        assertThat(box.getValue().getMaterial()).isEqualTo("Giấy kraft");
        assertThat(box.getValue().isActive()).isTrue();
        assertThat(box.getValue().getEmptyBoxVariant().getId()).isEqualTo(50L);
    }

    @Test
    void capacityOfBoxAlreadyUsedCannotChange() {
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", 51L);
        variant.setProduct(repo.product(2));
        variant.setSku("BOX-OLD");
        when(repo.variant(51L)).thenReturn(variant);
        Box box = new Box();
        ReflectionTestUtils.setField(box, "id", 7L);
        box.setCapacityCakes(4);
        when(repo.boxOfVariant(51L)).thenReturn(box);
        when(repo.presetUsesBox(7L)).thenReturn(true);
        VariantForm form = new VariantForm(51L, 2, "BOX-OLD", "Hộp 6 bánh", null, null, 6, null, BigDecimal.valueOf(30_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("không đổi được sức chứa");
        assertThat(box.getCapacityCakes()).isEqualTo(4);
    }

    @Test
    void cakeWithoutWeightIsRejected() {
        VariantForm form = new VariantForm(null, 1, "MC-NEW", "Bánh 150g", null, "Thập cẩm", null, null, BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("khối lượng");
        verify(repo, never()).save(any(com.se201411.project.mooncake.entity.ProductVariant.class));
    }

    @Test
    void cakeWithoutFillingIsRejected() {
        VariantForm form = new VariantForm(null, 1, "MC-NEW", "Bánh 150g", 150, " ", null, null, BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("nhân bánh");
    }

    @Test
    void priceMustBeWholeThousands() {
        VariantForm form = new VariantForm(null, 1, "MC-NEW", "Bánh 150g", 150, "Thập cẩm", null, null, BigDecimal.valueOf(65_500), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("bội số của 1.000đ");
    }

    @Test
    void duplicateSkuIsRejectedWithClearMessage() {
        when(repo.skuExists("MC-KSPM-080")).thenReturn(true);
        VariantForm form = new VariantForm(null, 1, "mc-kspm-080", "Bánh 80g", 80, "Kim sa", null, null, BigDecimal.valueOf(65_000), false);

        assertThatThrownBy(() -> service.saveVariant(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đã tồn tại");
    }

    @Test
    void skuWithSpacesOrAccentsIsRejected() {
        VariantForm form = new VariantForm(null, 1, "bánh mới", "Bánh 150g", 150, "Thập cẩm", null, null, BigDecimal.valueOf(65_000), false);

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

    @Test
    void mooncakeWithoutShelfLifeIsRejected() {
        ProductForm form = new ProductForm(null, 1, ProductType.MOONCAKE, "Bánh mới", "banh-moi", "Mô tả", null, null, null, null, true);

        assertThatThrownBy(() -> service.saveProduct(form))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("thời hạn sử dụng");
        verify(repo, never()).save(any(Product.class));
    }
}
