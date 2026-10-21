package com.se201411.project.mooncake;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.PresetComponentView;
import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class ProductCatalogIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductQueryService productQueryService;

    @Autowired
    private PresetConfigurationQueryService presetConfigurationQueryService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void seededKimPhucBoxShowsFourClearlyNamedCakes() {
        CatalogProductView giftBox = productQueryService
            .requireActiveProduct("hop-qua-kim-phuc");
        List<PresetComponentView> components = giftBox
            .getPrimaryVariant()
            .presetRecipe()
            .components();

        int totalCakeQuantity = 0;
        List<String> componentNames = new ArrayList<>();
        for (PresetComponentView component : components) {
            totalCakeQuantity += component.quantityPerBox();
            componentNames.add(component.name());
            assertThat(component.name()).startsWith("Bánh ");
            assertThat(component.name()).contains("(");
        }

        assertThat(components).hasSize(4);
        assertThat(totalCakeQuantity).isEqualTo(4);
        assertThat(componentNames)
            .doesNotHaveDuplicates()
            .contains(
                "Bánh Hạt Sen – Mắc Ca Trứng Muối (Bánh 120g)",
                "Bánh Trà Xanh – Hạnh Nhân Trứng Muối (Bánh 120g)",
                "Bánh Lạp Xưởng Gà Quay xốt XO Trứng Muối (Bánh 150g)",
                "Bánh Sò Điệp xốt Hồng Kông Trứng Muối (Bánh 150g)"
            );
    }

    @Test
    void readsActiveCatalogAndReadyPresetFromSqlServer() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Category category = new Category();
        category.setSlug("it-category-" + suffix);
        category.setName("Danh mục integration test");
        category.setActive(true);
        entityManager.persist(category);

        Product cakeProduct = product(
            category,
            ProductType.MOONCAKE,
            "it-cake-" + suffix,
            "Bánh integration test"
        );
        entityManager.persist(cakeProduct);
        ProductVariant cakeVariant = variant(
            cakeProduct,
            "IT-CAKE-" + suffix,
            "Bánh nhân thử nghiệm",
            125_000
        );
        cakeVariant.setWeightGrams(150);
        cakeVariant.setFillingLabel("Nhân kiểm thử");
        entityManager.persist(cakeVariant);
        persistBalance(cakeVariant, now);

        Product boxProduct = product(
            category,
            ProductType.EMPTY_BOX,
            "it-box-" + suffix,
            "Vỏ hộp integration test"
        );
        entityManager.persist(boxProduct);
        ProductVariant boxVariant = variant(
            boxProduct,
            "IT-BOX-" + suffix,
            "Vỏ hộp 4 bánh",
            200_000
        );
        entityManager.persist(boxVariant);
        persistBalance(boxVariant, now);

        Box box = new Box();
        box.setEmptyBoxVariant(boxVariant);
        box.setCapacityCakes(4);
        box.setActive(true);
        entityManager.persist(box);

        Product giftSet = product(
            category,
            ProductType.GIFT_SET,
            "it-gift-set-" + suffix,
            "Hộp quà integration test"
        );
        entityManager.persist(giftSet);
        ProductVariant readyVariant = variant(
            giftSet,
            "IT-GIFT-READY-" + suffix,
            "Phiên bản READY",
            880_000
        );
        entityManager.persist(readyVariant);
        persistBalance(readyVariant, now);

        ProductVariant variantWithoutPreset = variant(
            giftSet,
            "IT-GIFT-DRAFT-" + suffix,
            "Phiên bản chưa có PRESET",
            790_000
        );
        entityManager.persist(variantWithoutPreset);
        persistBalance(variantWithoutPreset, now);

        ProductImage image = new ProductImage();
        image.setVariant(readyVariant);
        image.setImageUrl("/images/integration-test-" + suffix + ".png");
        image.setAltText("Ảnh hộp quà integration test");
        image.setPrimary(true);
        image.setSortOrder(0);
        entityManager.persist(image);

        BoxConfiguration configuration = new BoxConfiguration();
        configuration.setBox(box);
        configuration.setPresetVariant(readyVariant);
        configuration.setType(ConfigurationType.PRESET);
        configuration.setStatus(ConfigurationStatus.READY);
        configuration.setCreatedAt(now);
        configuration.setUpdatedAt(now);
        entityManager.persist(configuration);

        BoxConfigurationItem component = new BoxConfigurationItem();
        component.setConfiguration(configuration);
        component.setMooncakeVariant(cakeVariant);
        component.setQuantityPerBox(4);
        entityManager.persist(component);

        entityManager.flush();
        entityManager.clear();

        List<CatalogProductView> catalog = productQueryService
            .findActiveCatalog(ProductType.GIFT_SET);
        CatalogProductView testProduct = catalog.stream()
            .filter(product -> product.getSlug().equals(giftSet.getSlug()))
            .findFirst()
            .orElseThrow();

        assertThat(testProduct.getVariants())
            .extracting(variant -> variant.sku())
            .containsExactly(readyVariant.getSku());
        assertThat(testProduct.getImageUrl()).isEqualTo(image.getImageUrl());
        assertThat(testProduct.getPrimaryVariant().presetRecipe().components())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.variantId()).isEqualTo(cakeVariant.getId());
                assertThat(item.name())
                    .isEqualTo("Bánh integration test (Bánh nhân thử nghiệm)");
                assertThat(item.quantityPerBox()).isEqualTo(4);
            });

        PresetRecipeSnapshot snapshot = presetConfigurationQueryService
            .requireReadyPreset(readyVariant.getId());
        assertThat(snapshot.configurationId()).isEqualTo(configuration.getId());
        assertThat(snapshot.components()).singleElement()
            .satisfies(item -> assertThat(item.quantityPerBox()).isEqualTo(4));

        assertThatThrownBy(() -> productQueryService.requireSellableVariant(
            variantWithoutPreset.getId()
        )).isInstanceOf(ResourceNotFoundException.class);

        mockMvc.perform(get("/products/{slug}", giftSet.getSlug()))
            .andExpect(status().isOk())
            .andExpect(view().name("product-detail"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(giftSet.getName())))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Bánh integration test (Bánh nhân thử nghiệm)"
            )))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Đăng nhập để thêm vào giỏ"
            )))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "data-preset-variant-id=\"" + readyVariant.getId() + "\""
            )));

        mockMvc.perform(get("/products/gift-boxes"))
            .andExpect(status().isOk())
            .andExpect(view().name("shop-catalog"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(giftSet.getName())))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Hộp 4 bánh")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "id=\"catalog-search\""
            )));

        mockMvc.perform(get("/products/mooncakes"))
            .andExpect(status().isOk())
            .andExpect(view().name("shop-catalog"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(cakeProduct.getName())))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhân kiểm thử")));
    }

    private Product product(
        Category category,
        ProductType type,
        String slug,
        String name
    ) {
        Product product = new Product();
        product.setCategory(category);
        product.setProductType(type);
        product.setSlug(slug);
        product.setName(name);
        product.setShortDescription("Dữ liệu kiểm thử catalog");
        product.setActive(true);
        return product;
    }

    private ProductVariant variant(
        Product product,
        String sku,
        String name,
        long price
    ) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setName(name);
        variant.setUnitPrice(BigDecimal.valueOf(price));
        variant.setActive(true);
        return variant;
    }

    private void persistBalance(ProductVariant variant, LocalDateTime now) {
        InventoryBalance balance = new InventoryBalance();
        balance.setVariant(variant);
        balance.setAvailableQuantity(20);
        balance.setLowStockThreshold(5);
        balance.setUpdatedAt(now);
        entityManager.persist(balance);
    }
}
