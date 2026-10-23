package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CartLineSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationItemRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationRepository;
import com.se201411.project.mooncake.cart.repository.VolumeDiscountTierRepository;
import com.se201411.project.mooncake.cart.service.impl.PricingServiceImpl;
import com.se201411.project.mooncake.entity.VolumeDiscountTier;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.DiscountGroup;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.dto.PresetComponentSnapshot;
import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock
    private ProductQueryService productQueryService;
    @Mock
    private PresetConfigurationQueryService presetQueryService;
    @Mock
    private CustomBoxConfigurationRepository configurationRepository;
    @Mock
    private CustomBoxConfigurationItemRepository configurationItemRepository;
    @Mock
    private VolumeDiscountTierRepository tierRepository;

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC);
        pricingService = new PricingServiceImpl(
            productQueryService,
            presetQueryService,
            configurationRepository,
            configurationItemRepository,
            tierRepository,
            clock
        );
    }

    @Test
    void appliesIndependentTiersAndHalfUpRoundingPerUnit() {
        when(productQueryService.requireSellableVariant(10L)).thenReturn(
            sellable(10L, "GIFT-10", ProductType.GIFT_SET, 590_010)
        );
        when(productQueryService.requireSellableVariant(20L)).thenReturn(
            sellable(20L, "CAKE-20", ProductType.MOONCAKE, 100_000)
        );
        when(productQueryService.requireSellableVariant(30L)).thenReturn(
            sellable(30L, "BOX-30", ProductType.EMPTY_BOX, 90_000)
        );

        when(presetQueryService.requireReadyPreset(10L)).thenReturn(
            new PresetRecipeSnapshot(
                100L,
                10L,
                30L,
                4,
                ConfigurationStatus.READY,
                List.of(new PresetComponentSnapshot(20L, 4))
            )
        );
        when(productQueryService.findVariantsForCart(any())).thenReturn(List.of(
            cartVariant(30L, "BOX-30", ProductType.EMPTY_BOX, 90_000),
            cartVariant(20L, "CAKE-20", ProductType.MOONCAKE, 100_000)
        ));

        VolumeDiscountTier giftTier = tier(DiscountGroup.GIFT_BOX, 10);
        VolumeDiscountTier cakeTier = tier(DiscountGroup.MOONCAKE, 10);
        when(tierRepository.findApplicableTiers(eq(DiscountGroup.GIFT_BOX), eq(55), any()))
            .thenReturn(List.of(giftTier));
        when(tierRepository.findApplicableTiers(eq(DiscountGroup.MOONCAKE), eq(105), any()))
            .thenReturn(List.of(cakeTier));

        CheckoutCartSnapshot cart = new CheckoutCartSnapshot(
            1L,
            ShopperIdentity.forCustomer(2L),
            List.of(
                new CartLineSnapshot(10L, null, 55),
                new CartLineSnapshot(20L, null, 105),
                new CartLineSnapshot(30L, null, 5)
            )
        );

        CheckoutQuote quote = pricingService.calculateCheckoutQuote(cart);

        assertThat(quote.lines().get(0).unitDiscount())
            .isEqualByComparingTo("59001");
        assertThat(quote.lines().get(1).discountPercent())
            .isEqualByComparingTo("10");
        assertThat(quote.lines().get(2).discountPercent())
            .isEqualByComparingTo("0");
        assertThat(quote.shippingFee()).isEqualByComparingTo("150000");
        assertThat(quote.roundingAdjustment()).isEqualByComparingTo("-495");
        assertThat(quote.total()).isEqualByComparingTo("39255000");
        assertThat(quote.groupSummaries()).hasSize(2);
    }

    @Test
    void chargesSmallShippingFeeWhenOrderHasNoGiftBoxes() {
        when(productQueryService.requireSellableVariant(20L)).thenReturn(
            sellable(20L, "CAKE-20", ProductType.MOONCAKE, 100_000)
        );
        when(tierRepository.findApplicableTiers(eq(DiscountGroup.MOONCAKE), eq(1), any()))
            .thenReturn(List.of());

        CheckoutCartSnapshot cart = new CheckoutCartSnapshot(
            1L,
            ShopperIdentity.forCustomer(2L),
            List.of(new CartLineSnapshot(20L, null, 1))
        );

        CheckoutQuote quote = pricingService.calculateCheckoutQuote(cart);

        assertThat(quote.shippingFee()).isEqualByComparingTo("30000");
        assertThat(quote.total()).isEqualByComparingTo("130000");
    }

    @Test
    void calculatesCustomBoxFromCurrentBoxAndCakePrices() {
        ProductVariant boxVariant = activeVariant(
            30L,
            "BOX-30",
            ProductType.EMPTY_BOX,
            100_000
        );
        ProductVariant cakeVariant = activeVariant(
            20L,
            "CAKE-20",
            ProductType.MOONCAKE,
            75_000
        );
        Box box = mock(Box.class);
        when(box.isActive()).thenReturn(true);
        when(box.getCapacityCakes()).thenReturn(4);
        when(box.getEmptyBoxVariant()).thenReturn(boxVariant);

        BoxConfiguration configuration = mock(BoxConfiguration.class);
        when(configuration.getStatus()).thenReturn(ConfigurationStatus.READY);
        when(configuration.getBox()).thenReturn(box);

        BoxConfigurationItem item = mock(BoxConfigurationItem.class);
        when(item.getMooncakeVariant()).thenReturn(cakeVariant);
        when(item.getQuantityPerBox()).thenReturn(4);

        when(configurationRepository.findOwnedConfiguration(
            50L,
            1L,
            ConfigurationType.CUSTOM
        )).thenReturn(Optional.of(configuration));
        when(configurationItemRepository.findByConfigurationIds(List.of(50L)))
            .thenReturn(List.of(item));
        when(tierRepository.findApplicableTiers(eq(DiscountGroup.GIFT_BOX), eq(2), any()))
            .thenReturn(List.of());

        CheckoutCartSnapshot cart = new CheckoutCartSnapshot(
            1L,
            ShopperIdentity.forCustomer(2L),
            List.of(new CartLineSnapshot(null, 50L, 2))
        );

        CheckoutQuote quote = pricingService.calculateCheckoutQuote(cart);

        assertThat(quote.lines().getFirst().unitBasePrice())
            .isEqualByComparingTo("400000");
        assertThat(quote.lines().getFirst().components()).hasSize(2);
        assertThat(quote.subtotal()).isEqualByComparingTo("800000");
    }

    private SellableVariantSnapshot sellable(
        long id,
        String sku,
        ProductType type,
        long price
    ) {
        return new SellableVariantSnapshot(
            id,
            id,
            sku,
            type,
            "Sản phẩm " + sku,
            BigDecimal.valueOf(price),
            true
        );
    }

    private CartVariantSnapshot cartVariant(
        long id,
        String sku,
        ProductType type,
        long price
    ) {
        return new CartVariantSnapshot(
            id,
            "slug-" + id,
            "Sản phẩm",
            sku,
            sku,
            type,
            BigDecimal.valueOf(price),
            "/image.png",
            sku,
            true
        );
    }

    private VolumeDiscountTier tier(DiscountGroup group, int percent) {
        VolumeDiscountTier tier = new VolumeDiscountTier();
        tier.setDiscountGroup(group);
        tier.setMinQuantity(1);
        tier.setDiscountPercent(BigDecimal.valueOf(percent));
        tier.setActive(true);
        return tier;
    }

    private ProductVariant activeVariant(
        long id,
        String sku,
        ProductType type,
        long price
    ) {
        Category category = mock(Category.class);
        when(category.isActive()).thenReturn(true);

        Product product = mock(Product.class);
        when(product.isActive()).thenReturn(true);
        when(product.getProductType()).thenReturn(type);
        when(product.getName()).thenReturn("Sản phẩm " + sku);
        when(product.getCategory()).thenReturn(category);

        ProductVariant variant = mock(ProductVariant.class);
        when(variant.getId()).thenReturn(id);
        when(variant.getName()).thenReturn(sku);
        when(variant.getUnitPrice()).thenReturn(BigDecimal.valueOf(price));
        when(variant.isActive()).thenReturn(true);
        when(variant.getProduct()).thenReturn(product);
        return variant;
    }
}
