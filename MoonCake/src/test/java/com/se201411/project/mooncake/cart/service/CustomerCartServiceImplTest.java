package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.service.impl.CustomerCartServiceImpl;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.CartStatus;
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
    void customConfigurationLineIsNotDeletedByStandardCartFlow() {
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

        assertThatThrownBy(() -> service.removeItem(CUSTOMER, 500L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("CUSTOM");

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
