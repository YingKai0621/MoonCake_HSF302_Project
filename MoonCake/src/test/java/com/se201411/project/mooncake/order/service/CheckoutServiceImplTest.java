package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.cart.dto.CartLineSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutComponentQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutLineQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.cart.service.PricingService;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.ComponentRole;
import com.se201411.project.mooncake.enums.DiscountGroup;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderItemKind;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.impl.CheckoutServiceImpl;
import com.se201411.project.mooncake.order.service.impl.DeliveryAddressServiceImpl;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    @Mock
    private CartService cartService;
    @Mock
    private PricingService pricingService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private CustomerProfileService customerProfileService;
    @Mock
    private SalesOrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderItemComponentRepository componentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EntityManager entityManager;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC);
        checkoutService = new CheckoutServiceImpl(
            cartService,
            pricingService,
            inventoryService,
            customerProfileService,
            new DeliveryAddressServiceImpl(new ObjectMapper()),
            orderRepository,
            orderItemRepository,
            componentRepository,
            paymentRepository,
            entityManager,
            clock
        );
    }

    @Test
    void presetUsesFinishedStockWhileCustomUsesComponents() {
        ShopperIdentity customerIdentity = ShopperIdentity.forCustomer(7L);
        CheckoutCartSnapshot cart = new CheckoutCartSnapshot(
            9L,
            customerIdentity,
            List.of(
                new CartLineSnapshot(10L, null, 2),
                new CartLineSnapshot(null, 50L, 3),
                new CartLineSnapshot(20L, null, 5)
            )
        );
        CheckoutQuote quote = new CheckoutQuote(
            List.of(
                line(cart.lines().get(0), OrderItemKind.STANDARD,
                    InventorySource.STOCKED_SKU, DiscountGroup.GIFT_BOX),
                line(cart.lines().get(1), OrderItemKind.CUSTOM,
                    InventorySource.COMPONENTS, DiscountGroup.GIFT_BOX),
                line(cart.lines().get(2), OrderItemKind.STANDARD,
                    InventorySource.STOCKED_SKU, DiscountGroup.MOONCAKE)
            ),
            List.of(),
            BigDecimal.valueOf(1_000_000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(30_000),
            BigDecimal.valueOf(1_030_000)
        );

        when(cartService.claimForCheckout(9L, customerIdentity)).thenReturn(cart);
        when(pricingService.calculateCheckoutQuote(cart)).thenReturn(quote);
        when(customerProfileService.requireProfile(customerIdentity)).thenReturn(
            new CustomerProfile(7L, "Khách hàng", "0901234567", "customer@example.com")
        );
        when(entityManager.getReference(Customer.class, 7L)).thenReturn(new Customer());
        when(entityManager.getReference(ProductVariant.class, 10L))
            .thenReturn(new ProductVariant());
        when(entityManager.getReference(ProductVariant.class, 20L))
            .thenReturn(new ProductVariant());
        when(entityManager.getReference(ProductVariant.class, 30L))
            .thenReturn(new ProductVariant());
        when(entityManager.getReference(BoxConfiguration.class, 50L))
            .thenReturn(new BoxConfiguration());
        when(orderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 99L);
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 100L);
            return item;
        });
        when(componentRepository.save(any(OrderItemComponent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutForm form = new CheckoutForm();
        form.setCartId(9L);
        form.setRecipientName("Người nhận");
        form.setRecipientPhone("0909999999");
        form.setProvinceCode("79");
        form.setWardCode("26740");
        form.setStreetAddress("123 Đường Trung Thu");

        checkoutService.placeOrder(customerIdentity, form);

        ArgumentCaptor<SaleInventoryCommand> commandCaptor = ArgumentCaptor.forClass(
            SaleInventoryCommand.class
        );
        org.mockito.Mockito.verify(inventoryService).deductForSale(commandCaptor.capture());
        SaleInventoryCommand command = commandCaptor.getValue();

        assertThat(command.orderId()).isEqualTo(99L);
        assertThat(command.demands()).extracting("variantId", "quantity")
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(10L, 2),
                org.assertj.core.groups.Tuple.tuple(30L, 3),
                org.assertj.core.groups.Tuple.tuple(20L, 17)
            );

        InOrder order = inOrder(cartService, orderRepository, inventoryService);
        order.verify(cartService).claimForCheckout(9L, customerIdentity);
        order.verify(orderRepository).save(any(SalesOrder.class));
        order.verify(inventoryService).deductForSale(any(SaleInventoryCommand.class));

        ArgumentCaptor<SalesOrder> salesOrderCaptor = ArgumentCaptor.forClass(SalesOrder.class);
        org.mockito.Mockito.verify(orderRepository).save(salesOrderCaptor.capture());
        assertThat(salesOrderCaptor.getValue().getShippingAddressSnapshot())
            .isEqualTo("123 Đường Trung Thu, Sài Gòn, Hồ Chí Minh");
    }

    private CheckoutLineQuote line(
        CartLineSnapshot cartLine,
        OrderItemKind itemKind,
        InventorySource inventorySource,
        DiscountGroup discountGroup
    ) {
        List<CheckoutComponentQuote> components = new ArrayList<>();
        String sku = "SKU-" + cartLine.variantId();
        String boxSku = null;
        if (discountGroup == DiscountGroup.GIFT_BOX) {
            components.add(new CheckoutComponentQuote(
                30L,
                ComponentRole.BOX,
                "Vỏ hộp",
                1,
                BigDecimal.valueOf(100_000)
            ));
            components.add(new CheckoutComponentQuote(
                20L,
                ComponentRole.CAKE,
                "Bánh",
                4,
                BigDecimal.valueOf(100_000)
            ));
            boxSku = "BOX-30";
        }
        if (itemKind == OrderItemKind.CUSTOM) {
            sku = null;
        }

        return new CheckoutLineQuote(
            cartLine,
            null,
            itemKind,
            inventorySource,
            "Sản phẩm",
            sku,
            discountGroup,
            boxSku == null ? null : "Vỏ hộp",
            boxSku,
            BigDecimal.valueOf(100_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(100_000L * cartLine.quantity()),
            components
        );
    }
}
