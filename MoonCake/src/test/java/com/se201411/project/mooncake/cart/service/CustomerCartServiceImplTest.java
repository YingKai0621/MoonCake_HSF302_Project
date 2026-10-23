package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationItemRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationRepository;
import com.se201411.project.mooncake.cart.service.impl.CustomerCartServiceImpl;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.CartStatus;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCartServiceImplTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(10L);

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CustomBoxConfigurationRepository configurationRepository;
    @Mock
    private CustomBoxConfigurationItemRepository configurationItemRepository;
    @Mock
    private ProductQueryService productQueryService;
    @Mock
    private EntityManager entityManager;

    private CustomerCartService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
            Instant.parse("2026-10-21T08:00:00Z"),
            ZoneOffset.UTC
        );
        service = new CustomerCartServiceImpl(
            cartRepository,
            cartItemRepository,
            configurationRepository,
            configurationItemRepository,
            productQueryService,
            entityManager,
            clock
        );
    }

    @Test
    void viewingCartWithoutActiveCartDoesNotCreateOne() {
        when(cartRepository.findByCustomerIdAndStatus(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.empty());

        CustomerCartView result = service.getCurrentCart(CUSTOMER);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.cartId()).isNull();
        verify(cartRepository, never()).createActiveCartIfMissing(10L);
    }

    @Test
    void firstAddCreatesActiveCartAndOneItem() {
        Cart cart = cart(100L);
        ProductVariant variantReference = variant(200L);
        SellableVariantSnapshot sellableVariant = new SellableVariantSnapshot(
            200L,
            300L,
            "MC-200",
            ProductType.MOONCAKE,
            "Bánh - Phiên bản",
            BigDecimal.valueOf(100_000),
            true
        );

        when(productQueryService.requireSellableVariant(200L)).thenReturn(sellableVariant);
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.empty(), Optional.of(cart));
        when(cartRepository.createActiveCartIfMissing(10L)).thenReturn(1);
        when(cartItemRepository.findVariantItem(100L, 200L)).thenReturn(Optional.empty());
        when(entityManager.getReference(ProductVariant.class, 200L))
            .thenReturn(variantReference);

        service.addVariant(CUSTOMER, 200L, 2);

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());
        CartItem savedItem = itemCaptor.getValue();
        assertThat(savedItem.getCart()).isSameAs(cart);
        assertThat(savedItem.getVariant()).isSameAs(variantReference);
        assertThat(savedItem.getQuantity()).isEqualTo(2);
        assertThat(savedItem.getAddedAt()).isNotNull();
        assertThat(cart.getUpdatedAt()).isNotNull();
    }

    @Test
    void addingSameVariantIncreasesExistingQuantity() {
        Cart cart = cart(100L);
        CartItem existingItem = item(500L, cart, variant(200L), 2);

        when(productQueryService.requireSellableVariant(200L)).thenReturn(
            new SellableVariantSnapshot(
                200L,
                300L,
                "MC-200",
                ProductType.MOONCAKE,
                "Bánh - Phiên bản",
                BigDecimal.valueOf(100_000),
                true
            )
        );
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartItemRepository.findVariantItem(100L, 200L))
            .thenReturn(Optional.of(existingItem));

        service.addVariant(CUSTOMER, 200L, 3);

        assertThat(existingItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository, never()).save(existingItem);
        verify(cartRepository, never()).createActiveCartIfMissing(10L);
    }

    @Test
    void cartViewUsesCurrentPriceAndMarksInactiveSku() {
        Cart cart = cart(100L);
        CartItem item = item(500L, cart, variant(200L), 3);

        when(cartRepository.findByCustomerIdAndStatus(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartItemRepository.findVariantItemsByCartId(100L)).thenReturn(List.of(item));
        when(productQueryService.findVariantsForCart(List.of(200L))).thenReturn(List.of(
            new CartVariantSnapshot(
                200L,
                "banh-lava",
                "Bánh Lava",
                "Bánh 120g",
                "MC-LAVA-120",
                ProductType.MOONCAKE,
                BigDecimal.valueOf(125_000),
                "/images/cake.png",
                "Bánh Lava",
                false
            )
        ));

        CustomerCartView result = service.getCurrentCart(CUSTOMER);

        assertThat(result.totalQuantity()).isEqualTo(3);
        assertThat(result.subtotal()).isEqualByComparingTo("375000");
        assertThat(result.hasUnavailableItems()).isTrue();
        assertThat(result.items()).singleElement().satisfies(view -> {
            assertThat(view.productName()).isEqualTo("Bánh Lava");
            assertThat(view.currentlySellable()).isFalse();
            assertThat(view.lineTotal()).isEqualByComparingTo("375000");
        });
    }

    @Test
    void customCartItemsUseImagesOfBothSelectedBoxVariants() {
        Cart cart = cart(100L);
        CartItem kimPhucItem = customItem(
            501L,
            cart,
            configuration(701L, box(401L, "BOX-KIM-PHUC-4", "Vỏ hộp Kim Phúc 4 bánh", 4))
        );
        CartItem lamPhucItem = customItem(
            502L,
            cart,
            configuration(702L, box(601L, "BOX-LAM-PHUC-6", "Vỏ hộp Lam Phúc 6 bánh", 6))
        );

        when(cartRepository.findByCustomerIdAndStatus(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartItemRepository.findVariantItemsByCartId(100L)).thenReturn(List.of());
        when(cartItemRepository.findCustomItemsByCartId(100L))
            .thenReturn(List.of(kimPhucItem, lamPhucItem));
        when(productQueryService.findVariantsForCart(List.of(401L, 601L)))
            .thenReturn(List.of(
                boxSnapshot(
                    401L,
                    "BOX-KIM-PHUC-4",
                    "Vỏ hộp Kim Phúc 4 bánh",
                    "/images/boxes/box-kim-phuc-4.png"
                ),
                boxSnapshot(
                    601L,
                    "BOX-LAM-PHUC-6",
                    "Vỏ hộp Lam Phúc 6 bánh",
                    "/images/boxes/box-lam-phuc-6.png"
                )
            ));
        when(configurationItemRepository.findByConfigurationIds(List.of(701L, 702L)))
            .thenReturn(List.of());

        CustomerCartView result = service.getCurrentCart(CUSTOMER);

        assertThat(result.items())
            .extracting(item -> item.imageUrl())
            .containsExactly(
                "/images/boxes/box-kim-phuc-4.png",
                "/images/boxes/box-lam-phuc-6.png"
            );
    }

    @Test
    void customerCannotUpdateAnItemOutsideTheirActiveCart() {
        Cart cart = cart(100L);
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(999L, 100L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItemQuantity(CUSTOMER, 999L, 2))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("giỏ hàng của bạn");
    }

    @Test
    void increasingQuantityRevalidatesThatSkuIsStillSellable() {
        Cart cart = cart(100L);
        CartItem item = item(500L, cart, variant(200L), 2);
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(500L, 100L))
            .thenReturn(Optional.of(item));
        when(productQueryService.requireSellableVariant(200L))
            .thenThrow(new ResourceNotFoundException("SKU đã ngừng bán"));

        assertThatThrownBy(() -> service.updateItemQuantity(CUSTOMER, 500L, 4))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("ngừng bán");
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void customConfigurationOutsideOwnedCartCannotBeDeleted() {
        Cart cart = cart(100L);
        CartItem item = new CartItem();
        setId(item, 500L);
        item.setCart(cart);
        item.setConfigurationId(700L);
        item.setQuantity(1);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(500L, 100L))
            .thenReturn(Optional.of(item));
        when(configurationRepository.findOwnedConfiguration(
            700L,
            100L,
            com.se201411.project.mooncake.enums.ConfigurationType.CUSTOM
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeItem(CUSTOMER, 500L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("giỏ hàng của bạn");

        verify(cartItemRepository, never()).delete(item);
    }

    private Cart cart(long id) {
        Cart cart = new Cart();
        setId(cart, id);
        cart.setStatus(CartStatus.ACTIVE);
        return cart;
    }

    private ProductVariant variant(long id) {
        ProductVariant variant = new ProductVariant();
        setId(variant, id);
        return variant;
    }

    private ProductVariant box(
        long id,
        String sku,
        String productName,
        int capacity
    ) {
        Category category = new Category();
        category.setActive(true);

        Product product = new Product();
        product.setName(productName);
        product.setSlug(sku.toLowerCase());
        product.setProductType(ProductType.EMPTY_BOX);
        product.setCategory(category);
        product.setActive(true);

        ProductVariant variant = variant(id);
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setName(productName);
        variant.setUnitPrice(capacity == 4
            ? BigDecimal.valueOf(495_000)
            : BigDecimal.valueOf(590_000));
        variant.setActive(true);
        return variant;
    }

    private BoxConfiguration configuration(long id, ProductVariant boxVariant) {
        Box box = new Box();
        box.setEmptyBoxVariant(boxVariant);
        box.setCapacityCakes(boxVariant.getSku().endsWith("-4") ? 4 : 6);
        box.setActive(true);

        BoxConfiguration configuration = new BoxConfiguration();
        setId(configuration, id);
        configuration.setBox(box);
        configuration.setType(ConfigurationType.CUSTOM);
        configuration.setStatus(ConfigurationStatus.READY);
        return configuration;
    }

    private CartItem customItem(
        long id,
        Cart cart,
        BoxConfiguration configuration
    ) {
        CartItem item = new CartItem();
        setId(item, id);
        item.setCart(cart);
        item.setConfiguration(configuration);
        item.setQuantity(1);
        return item;
    }

    private CartVariantSnapshot boxSnapshot(
        long id,
        String sku,
        String productName,
        String imageUrl
    ) {
        return new CartVariantSnapshot(
            id,
            sku.toLowerCase(),
            productName,
            productName,
            sku,
            ProductType.EMPTY_BOX,
            sku.endsWith("-4")
                ? BigDecimal.valueOf(495_000)
                : BigDecimal.valueOf(590_000),
            imageUrl,
            productName,
            true
        );
    }

    private CartItem item(
        long id,
        Cart cart,
        ProductVariant variant,
        int quantity
    ) {
        CartItem item = new CartItem();
        setId(item, id);
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private void setId(Object target, long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
