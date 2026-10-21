package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CustomCakeQuantity;
import com.se201411.project.mooncake.cart.dto.SaveCustomBoxCommand;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationItemRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxRepository;
import com.se201411.project.mooncake.cart.service.impl.CustomBoxServiceImpl;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.CartStatus;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomBoxServiceImplTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(10L);

    @Mock
    private CustomBoxRepository customBoxRepository;
    @Mock
    private CustomBoxConfigurationRepository configurationRepository;
    @Mock
    private CustomBoxConfigurationItemRepository configurationItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductQueryService productQueryService;
    @Mock
    private EntityManager entityManager;

    private CustomBoxService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
            Instant.parse("2026-10-21T08:00:00Z"),
            ZoneOffset.UTC
        );
        service = new CustomBoxServiceImpl(
            customBoxRepository,
            configurationRepository,
            configurationItemRepository,
            cartRepository,
            cartItemRepository,
            productQueryService,
            entityManager,
            clock
        );
    }

    @Test
    void createsReadyConfigurationWithAggregatedCakeComponents() {
        Cart cart = cart(100L);
        Box box = box(20L, 4);
        ProductVariant firstCake = variant(201L);
        ProductVariant secondCake = variant(202L);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(customBoxRepository.findSellableBoxById(20L, ProductType.EMPTY_BOX))
            .thenReturn(Optional.of(box));
        when(productQueryService.requireSellableVariant(201L))
            .thenReturn(mooncake(201L));
        when(productQueryService.requireSellableVariant(202L))
            .thenReturn(mooncake(202L));
        when(entityManager.getReference(ProductVariant.class, 201L)).thenReturn(firstCake);
        when(entityManager.getReference(ProductVariant.class, 202L)).thenReturn(secondCake);
        when(configurationRepository.save(any(BoxConfiguration.class))).thenAnswer(invocation -> {
            BoxConfiguration configuration = invocation.getArgument(0);
            ReflectionTestUtils.setField(configuration, "id", 300L);
            return configuration;
        });

        SaveCustomBoxCommand command = new SaveCustomBoxCommand(
            20L,
            2,
            List.of(
                new CustomCakeQuantity(201L, 1),
                new CustomCakeQuantity(201L, 1),
                new CustomCakeQuantity(202L, 2)
            )
        );

        long configurationId = service.createAndAddToCart(CUSTOMER, command);

        assertThat(configurationId).isEqualTo(300L);

        ArgumentCaptor<BoxConfiguration> configurationCaptor =
            ArgumentCaptor.forClass(BoxConfiguration.class);
        verify(configurationRepository).save(configurationCaptor.capture());
        BoxConfiguration configuration = configurationCaptor.getValue();
        assertThat(configuration.getCart()).isSameAs(cart);
        assertThat(configuration.getType()).isEqualTo(ConfigurationType.CUSTOM);
        assertThat(configuration.getStatus()).isEqualTo(ConfigurationStatus.READY);
        assertThat(configuration.getPresetVariant()).isNull();

        ArgumentCaptor<BoxConfigurationItem> componentCaptor =
            ArgumentCaptor.forClass(BoxConfigurationItem.class);
        verify(configurationItemRepository, org.mockito.Mockito.times(2))
            .save(componentCaptor.capture());
        assertThat(componentCaptor.getAllValues())
            .extracting(BoxConfigurationItem::getQuantityPerBox)
            .containsExactly(2, 2);

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getConfigurationId()).isEqualTo(300L);
        assertThat(cartItemCaptor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsConfigurationThatDoesNotFillTheSelectedBox() {
        Cart cart = cart(100L);
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(customBoxRepository.findSellableBoxById(20L, ProductType.EMPTY_BOX))
            .thenReturn(Optional.of(box(20L, 4)));

        SaveCustomBoxCommand command = new SaveCustomBoxCommand(
            20L,
            1,
            List.of(new CustomCakeQuantity(201L, 3))
        );

        assertThatThrownBy(() -> service.createAndAddToCart(CUSTOMER, command))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đúng 4 bánh");

        verify(productQueryService, never()).requireSellableVariant(201L);
        verify(configurationRepository, never()).save(any(BoxConfiguration.class));
    }

    @Test
    void rejectsNonMooncakeComponentEvenWhenCapacityMatches() {
        Cart cart = cart(100L);
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(customBoxRepository.findSellableBoxById(20L, ProductType.EMPTY_BOX))
            .thenReturn(Optional.of(box(20L, 4)));
        when(productQueryService.requireSellableVariant(999L)).thenReturn(
            new SellableVariantSnapshot(
                999L,
                50L,
                "BOX-INVALID",
                ProductType.EMPTY_BOX,
                "Vỏ hộp",
                BigDecimal.valueOf(100_000),
                true
            )
        );

        SaveCustomBoxCommand command = new SaveCustomBoxCommand(
            20L,
            1,
            List.of(new CustomCakeQuantity(999L, 4))
        );

        assertThatThrownBy(() -> service.createAndAddToCart(CUSTOMER, command))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("chỉ được chứa SKU bánh");
    }

    @Test
    void cannotUpdateConfigurationOutsideCustomersActiveCart() {
        Cart cart = cart(100L);
        when(cartRepository.findByCustomerIdAndStatusForUpdate(10L, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(configurationRepository.findOwnedConfiguration(
            300L,
            100L,
            ConfigurationType.CUSTOM
        )).thenReturn(Optional.empty());

        SaveCustomBoxCommand command = new SaveCustomBoxCommand(
            20L,
            1,
            List.of(new CustomCakeQuantity(201L, 4))
        );

        assertThatThrownBy(() -> service.updateConfiguration(CUSTOMER, 300L, command))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("giỏ hàng của bạn");

        verify(customBoxRepository, never()).findSellableBoxById(
            org.mockito.ArgumentMatchers.anyLong(),
            any(ProductType.class)
        );
        verify(configurationItemRepository, never()).deleteAll(any());
    }

    private Cart cart(long id) {
        Cart cart = new Cart();
        ReflectionTestUtils.setField(cart, "id", id);
        cart.setStatus(CartStatus.ACTIVE);
        return cart;
    }

    private Box box(long id, int capacity) {
        Box box = new Box();
        ReflectionTestUtils.setField(box, "id", id);
        box.setCapacityCakes(capacity);
        return box;
    }

    private ProductVariant variant(long id) {
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        return variant;
    }

    private SellableVariantSnapshot mooncake(long id) {
        return new SellableVariantSnapshot(
            id,
            id + 1000,
            "MC-" + id,
            ProductType.MOONCAKE,
            "Bánh Trung Thu",
            BigDecimal.valueOf(100_000),
            true
        );
    }
}
