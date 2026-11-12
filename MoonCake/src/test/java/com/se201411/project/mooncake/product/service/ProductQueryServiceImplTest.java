package com.se201411.project.mooncake.product.service;

import java.util.Map;
import java.time.LocalDate;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.product.dto.ProductDetailPageView;
import com.se201411.project.mooncake.product.repository.ProductVideoRepository;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogPageView;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.repository.PresetConfigurationItemRepository;
import com.se201411.project.mooncake.product.repository.PresetConfigurationRepository;
import com.se201411.project.mooncake.product.repository.ProductImageRepository;
import com.se201411.project.mooncake.product.repository.ProductRepository;
import com.se201411.project.mooncake.product.repository.ProductVariantRepository;
import com.se201411.project.mooncake.product.service.impl.ProductQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductVideoRepository productVideoRepository;
    @Mock
    private PresetConfigurationRepository presetConfigurationRepository;
    @Mock
    private PresetConfigurationItemRepository presetConfigurationItemRepository;
    @Mock
    private InventoryService inventoryService;

    private ProductQueryService service;

    @BeforeEach
    void setUp() {
        service = new ProductQueryServiceImpl(
            productRepository,
            productVariantRepository,
            productImageRepository,
            productVideoRepository,
            presetConfigurationRepository,
            presetConfigurationItemRepository,
            inventoryService
        );
    }

    @Test
    void giftSetCatalogIncludesOnlyVariantsWithReadyPreset() {
        Category category = category(1L);
        Product giftSet = product(10L, category, ProductType.GIFT_SET, "hop-trang-vang");
        ProductVariant readyVariant = variant(101L, giftSet, "GIFT-READY", "Hộp 4 bánh", 880_000);
        ProductVariant noRecipeVariant = variant(102L, giftSet, "GIFT-DRAFT", "Hộp 6 bánh", 1_200_000);

        Product emptyBoxProduct = product(20L, category, ProductType.EMPTY_BOX, "vo-hop-son-mai");
        ProductVariant boxVariant = variant(201L, emptyBoxProduct, "BOX-4", "Vỏ hộp 4 bánh", 200_000);
        Box box = new Box();
        setId(box, 301L);
        box.setEmptyBoxVariant(boxVariant);
        box.setCapacityCakes(4);

        BoxConfiguration configuration = new BoxConfiguration();
        setId(configuration, 401L);
        configuration.setBox(box);
        configuration.setPresetVariant(readyVariant);
        configuration.setType(ConfigurationType.PRESET);
        configuration.setStatus(ConfigurationStatus.READY);

        Product cakeProduct = product(30L, category, ProductType.MOONCAKE, "banh-lava");
        ProductVariant cakeVariant = variant(501L, cakeProduct, "CAKE-LAVA", "Lava trứng muối", 135_000);
        BoxConfigurationItem component = new BoxConfigurationItem();
        component.setConfiguration(configuration);
        component.setMooncakeVariant(cakeVariant);
        component.setQuantityPerBox(4);

        when(productRepository.findActiveByType(ProductType.GIFT_SET)).thenReturn(List.of(giftSet));
        when(productVariantRepository.findActiveByProductIds(any()))
            .thenReturn(List.of(readyVariant, noRecipeVariant));
        when(presetConfigurationRepository.findReadyByGiftSetVariantIds(
            any(),
            any(),
            any()
        )).thenReturn(List.of(configuration));
        when(productImageRepository.findOrderedByVariantIds(any())).thenReturn(List.of());
        when(presetConfigurationItemRepository.findByConfigurationIds(any()))
            .thenReturn(List.of(component));

        List<CatalogProductView> products = service.findActiveCatalog(ProductType.GIFT_SET);

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().getVariants()).hasSize(1);
        assertThat(products.getFirst().getPrimaryVariant().id()).isEqualTo(readyVariant.getId());
        assertThat(products.getFirst().getPrimaryVariant().presetRecipe().capacityCakes()).isEqualTo(4);
        assertThat(products.getFirst().getPrimaryVariant().presetRecipe().components())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.variantId()).isEqualTo(cakeVariant.getId());
                assertThat(item.name())
                    .isEqualTo("Sản phẩm banh-lava (Lava trứng muối)");
                assertThat(item.quantityPerBox()).isEqualTo(4);
            });

        CatalogPageView page = service.giftBoxCatalogPage();

        assertThat(page.category()).isEqualTo("gift-boxes");
        assertThat(page.title()).isEqualTo("Hộp quà Trung Thu");
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.boxCountOptions()).containsExactly(4);
        assertThat(page.fillingOptions()).isEmpty();
    }

    @Test
    void giftSetVariantWithoutReadyPresetIsNotSellable() {
        Category category = category(1L);
        Product giftSet = product(10L, category, ProductType.GIFT_SET, "hop-chua-ready");
        ProductVariant variant = variant(101L, giftSet, "GIFT-DRAFT", "Hộp nháp", 500_000);

        when(productVariantRepository.findSellableById(variant.getId()))
            .thenReturn(Optional.of(variant));
        when(presetConfigurationRepository.findReadyByGiftSetVariantId(
            variant.getId(),
            ConfigurationType.PRESET,
            ConfigurationStatus.READY
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireSellableVariant(variant.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("PRESET READY");
    }

    @Test
    void cartReadKeepsInactiveVariantVisibleButMarksItUnavailable() {
        Category category = category(1L);
        Product product = product(10L, category, ProductType.MOONCAKE, "banh-cu");
        ProductVariant variant = variant(101L, product, "MC-CU", "Bánh 120g", 120_000);
        variant.setActive(false);

        when(productVariantRepository.findByIdsForCart(List.of(variant.getId())))
            .thenReturn(List.of(variant));
        when(productImageRepository.findOrderedByVariantIds(List.of(variant.getId())))
            .thenReturn(List.of());

        List<CartVariantSnapshot> result = service.findVariantsForCart(
            List.of(variant.getId())
        );

        assertThat(result).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.productSlug()).isEqualTo("banh-cu");
            assertThat(snapshot.sku()).isEqualTo("MC-CU");
            assertThat(snapshot.currentlySellable()).isFalse();
        });
    }

    @Test
    void productDetailPageCarriesEveryGalleryImage() {
        Category category = category(1L);
        Product cake = product(30L, category, ProductType.MOONCAKE, "banh-cha-bong");
        ProductVariant cakeVariant = variant(501L, cake, "MC-CB-80", "Bánh 80g", 105_000);
        when(productRepository.findActiveBySlug("banh-cha-bong")).thenReturn(Optional.of(cake));
        when(productRepository.findActiveByType(ProductType.MOONCAKE)).thenReturn(List.of(cake));
        when(productVariantRepository.findActiveByProductIds(any())).thenReturn(List.of(cakeVariant));
        when(productImageRepository.findOrderedByVariantIds(any())).thenReturn(List.of());
        when(productImageRepository.findGalleryByProductId(30L)).thenReturn(List.of(
            image(cakeVariant, "/images/cake-1.jpg"),
            image(cakeVariant, "/images/cake-2.jpg"),
            image(cakeVariant, "/images/cake-3.jpg")
        ));
        when(productVideoRepository.findByProductIdOrderBySortOrderAscIdAsc(30L)).thenReturn(List.of());

        ProductDetailPageView page = service.productDetailPage("banh-cha-bong");

        assertThat(page.galleryImages()).extracting("url")
            .containsExactly("/images/cake-1.jpg", "/images/cake-2.jpg", "/images/cake-3.jpg");
        assertThat(page.galleryVideos()).isEmpty();
    }

    private ProductImage image(ProductVariant variant, String url) {
        ProductImage image = new ProductImage();
        image.setVariant(variant);
        image.setImageUrl(url);
        image.setAltText("Bánh");
        return image;
    }

    private Category category(long id) {
        Category category = new Category();
        setId(category, id);
        category.setSlug("hop-qua");
        category.setName("Hộp quà");
        category.setActive(true);
        return category;
    }

    private Product product(
        long id,
        Category category,
        ProductType type,
        String slug
    ) {
        Product product = new Product();
        setId(product, id);
        product.setCategory(category);
        product.setProductType(type);
        product.setSlug(slug);
        product.setName("Sản phẩm " + slug);
        product.setShortDescription("Mô tả đã xác minh");
        product.setActive(true);
        return product;
    }

    private ProductVariant variant(
        long id,
        Product product,
        String sku,
        String name,
        long price
    ) {
        ProductVariant variant = new ProductVariant();
        setId(variant, id);
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setName(name);
        variant.setUnitPrice(BigDecimal.valueOf(price));
        variant.setActive(true);
        return variant;
    }

    private void setId(Object target, long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
