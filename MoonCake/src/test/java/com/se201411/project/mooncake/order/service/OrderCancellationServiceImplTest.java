package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.impl.CodPaymentProcessor;
import com.se201411.project.mooncake.order.service.impl.OrderCancellationServiceImpl;
import com.se201411.project.mooncake.order.service.impl.PaymentProcessorRegistryImpl;
import com.se201411.project.mooncake.order.service.impl.VnpayPaymentProcessor;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCancellationServiceImplTest {

    private final SalesOrderRepository orderRepository = mock(SalesOrderRepository.class);
    private final OrderItemRepository itemRepository = mock(OrderItemRepository.class);
    private final OrderItemComponentRepository componentRepository = mock(
        OrderItemComponentRepository.class
    );
    private final InventoryService inventoryService = mock(InventoryService.class);
    private final CurrentCustomerService currentCustomerService = mock(
        CurrentCustomerService.class
    );
    private final AdminAccessService adminAccessService = mock(AdminAccessService.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final VnpayPaymentService vnpayPaymentService = mock(VnpayPaymentService.class);
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-10-22T10:00:00Z"),
        ZoneOffset.UTC
    );
    private final OrderCancellationService service = new OrderCancellationServiceImpl(
        orderRepository,
        itemRepository,
        componentRepository,
        paymentRepository,
        new PaymentProcessorRegistryImpl(List.of(
            new CodPaymentProcessor(paymentRepository),
            new VnpayPaymentProcessor(vnpayPaymentService)
        )),
        inventoryService,
        currentCustomerService,
        adminAccessService,
        clock
    );

    @BeforeEach
    void allowAdminAndUseCodByDefault() {
        when(adminAccessService.requireAdmin()).thenReturn(99L);
        when(paymentRepository.findByOrderId(anyLong()))
            .thenReturn(List.of(payment(PaymentMethod.COD, PaymentStatus.UNPAID)));
    }

    @Test
    void cancellationLocksOrderBeforeChangingIt() {
        SalesOrder order = order(5L, OrderStatus.PLACED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.cancelFromStatus(
            eq(5L),
            eq(OrderStatus.PLACED),
            eq(OrderStatus.CANCELLED),
            any()
        )).thenReturn(1);
        OrderItem standard = item(51L, InventorySource.STOCKED_SKU, 1);
        standard.setVariant(variant(30L));
        when(itemRepository.findByOrderId(5L)).thenReturn(List.of(standard));

        service.cancelForAdmin(5L);

        InOrder inOrder = inOrder(orderRepository, paymentRepository, inventoryService);
        inOrder.verify(orderRepository).findByIdForUpdate(5L);
        inOrder.verify(paymentRepository).findByOrderId(5L);
        inOrder.verify(orderRepository).cancelFromStatus(eq(5L), any(), any(), any());
        inOrder.verify(inventoryService).restoreForCancellation(any());
    }

    @Test
    void vnpayOrderIsRejectedWithoutChangingStatusOrStock() {
        SalesOrder order = order(6L, OrderStatus.PLACED);
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(6L))
            .thenReturn(List.of(payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID)));

        assertThatThrownBy(() -> service.cancelForAdmin(6L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("VNPay");

        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void orderWithoutExactlyOnePaymentIsRejected() {
        SalesOrder order = order(7L, OrderStatus.PLACED);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.cancelForAdmin(7L))
            .isInstanceOf(BusinessRuleViolationException.class);

        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void adminCancellationRestoresStockedSkuAndCustomSnapshotsOnce() {
        SalesOrder order = order(1L, OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.cancelFromStatus(
            eq(1L),
            eq(OrderStatus.PROCESSING),
            eq(OrderStatus.CANCELLED),
            any()
        )).thenReturn(1);

        OrderItem standard = item(11L, InventorySource.STOCKED_SKU, 2);
        standard.setVariant(variant(10L));
        OrderItem custom = item(12L, InventorySource.COMPONENTS, 3);
        OrderItemComponent cake = component(custom, 10L, 4);
        OrderItemComponent box = component(custom, 20L, 1);
        when(itemRepository.findByOrderId(1L)).thenReturn(List.of(standard, custom));
        when(componentRepository.findByOrderItemIds(List.of(11L, 12L)))
            .thenReturn(List.of(cake, box));

        service.cancelForAdmin(1L);

        ArgumentCaptor<CancellationInventoryCommand> command = ArgumentCaptor.forClass(
            CancellationInventoryCommand.class
        );
        verify(inventoryService).restoreForCancellation(command.capture());
        assertThat(command.getValue().operationKey()).isEqualTo("CANCEL_RETURN:1");
        assertThat(command.getValue().demands())
            .extracting("variantId", "quantity")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(10L, 14),
                org.assertj.core.groups.Tuple.tuple(20L, 3)
            );
    }

    @Test
    void customerCancellationUsesCurrentCustomerAndOnlyPlacedOrder() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(8L);
        SalesOrder order = order(2L, OrderStatus.PLACED);
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(customer);
        when(orderRepository.findOwnedByOrderCode("MC-2", 8L))
            .thenReturn(Optional.of(order));
        when(orderRepository.cancelFromStatus(
            eq(2L),
            eq(OrderStatus.PLACED),
            eq(OrderStatus.CANCELLED),
            any()
        )).thenReturn(1);

        OrderItem standard = item(21L, InventorySource.STOCKED_SKU, 1);
        standard.setVariant(variant(30L));
        when(itemRepository.findByOrderId(2L)).thenReturn(List.of(standard));
        when(componentRepository.findByOrderItemIds(List.of(21L)))
            .thenReturn(List.of());

        service.cancelCurrentCustomerOrder(" MC-2 ");

        verify(orderRepository).findOwnedByOrderCode("MC-2", 8L);
        verify(inventoryService).restoreForCancellation(any());
        verifyNoInteractions(adminAccessService);
    }

    @Test
    void lostConditionalTransitionDoesNotRestoreInventory() {
        SalesOrder order = order(3L, OrderStatus.PLACED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelForAdmin(3L))
            .isInstanceOf(BusinessRuleViolationException.class);

        verify(inventoryService, never()).restoreForCancellation(any());
        verifyNoInteractions(itemRepository, componentRepository);
    }

    @Test
    void adminCannotCancelShippingOrder() {
        SalesOrder order = order(4L, OrderStatus.SHIPPING);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelForAdmin(4L))
            .isInstanceOf(BusinessRuleViolationException.class);

        verify(orderRepository, never()).cancelFromStatus(
            anyLong(),
            any(),
            any(),
            any()
        );
        verifyNoInteractions(inventoryService);
    }

    private SalesOrder order(long id, OrderStatus status) {
        SalesOrder order = new SalesOrder();
        ReflectionTestUtils.setField(order, "id", id);
        order.setStatus(status);
        return order;
    }

    private OrderItem item(long id, InventorySource source, int quantity) {
        OrderItem item = new OrderItem();
        ReflectionTestUtils.setField(item, "id", id);
        item.setInventorySourceSnapshot(source);
        item.setQuantity(quantity);
        return item;
    }

    private OrderItemComponent component(
        OrderItem orderItem,
        long variantId,
        int quantityPerSet
    ) {
        OrderItemComponent component = new OrderItemComponent();
        component.setOrderItem(orderItem);
        component.setComponentVariant(variant(variantId));
        component.setQuantityPerSet(quantityPerSet);
        return component;
    }

    private Payment payment(PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setStatus(status);
        return payment;
    }

    private ProductVariant variant(long id) {
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        return variant;
    }
}
