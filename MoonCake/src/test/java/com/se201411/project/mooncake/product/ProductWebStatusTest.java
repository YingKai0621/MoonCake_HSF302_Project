package com.se201411.project.mooncake.product;

import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Balance;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.product.dto.AdminProductRow;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductWebStatusTest {
    private final ProductAdminRepository repo = mock(ProductAdminRepository.class);
    private final InventoryAdminService inventoryAdmin = mock(InventoryAdminService.class);
    private final PresetConfigurationRepository presetConfigurations = mock(PresetConfigurationRepository.class);
    private final List<Product> products = new ArrayList<>();
    private final List<ProductVariant> variants = new ArrayList<>();
    private final List<Balance> balances = new ArrayList<>();
    private ProductAdminServiceImpl service;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ProductAdminServiceImpl(
            repo,
            mock(InventoryService.class),
            mock(PresetConfigurationQueryService.class),
            mock(AdminAccessService.class),
            mock(AdminFormValidation.class),
            mock(ProductImageStorageService.class),
            inventoryAdmin,
            presetConfigurations
        );
        category = new Category();
        category.setName("Bánh trung thu");
        category.setActive(true);
        when(repo.products()).thenReturn(products);
        when(repo.variants()).thenReturn(variants);
        when(repo.images()).thenReturn(List.of());
        when(inventoryAdmin.balances()).thenReturn(balances);
        when(presetConfigurations.findReadyByGiftSetVariantIds(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void cakeWithStockIsOnTheWeb() {
        Product cake = product(1L, ProductType.MOONCAKE, true);
        variant(11L, cake, true, 5);

        assertThat(onlyRow().webLabel()).isEqualTo("Đang bán trên web");
    }

    @Test
    void openCakeWithoutStockIsReportedAsOutOfStock() {
        Product cake = product(1L, ProductType.MOONCAKE, true);
        variant(11L, cake, true, 0);

        assertThat(onlyRow().webLabel()).isEqualTo("Kho đang hết");
    }

    @Test
    void giftSetWithoutReadyRecipeIsNotOnTheWeb() {
        Product gift = product(2L, ProductType.GIFT_SET, true);
        variant(21L, gift, true, 10);

        assertThat(onlyRow().webLabel()).isEqualTo("Chưa có công thức");
    }

    @Test
    void hiddenProductWinsOverEverythingElse() {
        Product cake = product(1L, ProductType.MOONCAKE, false);
        variant(11L, cake, true, 5);

        assertThat(onlyRow().webLabel()).isEqualTo("Đang ẩn");
    }

    @Test
    void productWithoutOpenVersionIsNotOnSale() {
        Product cake = product(1L, ProductType.MOONCAKE, true);
        variant(11L, cake, false, 5);

        assertThat(onlyRow().webLabel()).isEqualTo("Chưa mở bán");
    }

    private AdminProductRow onlyRow() {
        List<AdminProductRow> rows = service.productRows(null);
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    private Product product(long id, ProductType type, boolean active) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("Sản phẩm " + id);
        product.setProductType(type);
        product.setCategory(category);
        product.setActive(active);
        products.add(product);
        return product;
    }

    private void variant(long id, Product product, boolean active, int stock) {
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        variant.setProduct(product);
        variant.setName("Phiên bản " + id);
        variant.setUnitPrice(BigDecimal.valueOf(100_000));
        variant.setActive(active);
        variants.add(variant);
        balances.add(new Balance(id, "SKU-" + id, "Phiên bản " + id, stock, 5));
    }
}
