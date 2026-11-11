package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.security.service.CustomerProfileService;
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
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.doAnswer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private final CustomerProfileService customerProfileService = mock(CustomerProfileService.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentProcessor vnpayProcessor = vnpayProcessorMock();
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
            vnpayProcessor
        )),
        inventoryService,
        currentCustomerService,
        adminAccessService,
        customerProfileService,
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

        when(adminAccessService.requireAdmin()).thenReturn(99L);

        service.cancelForAdmin(5L);

        ArgumentCaptor<CancellationInventoryCommand> recorded = ArgumentCaptor.forClass(CancellationInventoryCommand.class);
        verify(inventoryService).restoreForCancellation(recorded.capture());
        assertThat(recorded.getValue().performedByAccountId()).isEqualTo(99L);
        InOrder inOrder = inOrder(orderRepository, paymentRepository, inventoryService);
        inOrder.verify(orderRepository).findByIdForUpdate(5L);
        inOrder.verify(paymentRepository).findByOrderId(5L);
        inOrder.verify(orderRepository).cancelFromStatus(eq(5L), any(), any(), any());
        inOrder.verify(inventoryService).restoreForCancellation(any());
    }

    @Test
    void vnpayOrderWithOpenAttemptIsRejectedWithoutChangingStatusOrStock() {
        SalesOrder order = order(6L, OrderStatus.PLACED);
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(6L))
            .thenReturn(List.of(payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID)));
        doThrow(new BusinessRuleViolationException("Đơn đang có giao dịch VNPay chưa kết thúc."))
            .when(vnpayProcessor).checkCanCancel(eq(6L), any());

        assertThatThrownBy(() -> service.cancelForAdmin(6L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("VNPay");

        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void customerCancelsUnpaidVnpayOrderOnceNoAttemptIsOpen() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(8L);
        SalesOrder order = order(13L, OrderStatus.PLACED);
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(customer);
        when(orderRepository.findOwnedByOrderCode("MC-13", 8L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(13L))
            .thenReturn(List.of(payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID)));
        when(orderRepository.cancelFromStatus(
            eq(13L), eq(OrderStatus.PLACED), eq(OrderStatus.CANCELLED), any()
        )).thenReturn(1);
        OrderItem standard = item(131L, InventorySource.STOCKED_SKU, 2);
        standard.setVariant(variant(30L));
        when(itemRepository.findByOrderId(13L)).thenReturn(List.of(standard));

        service.cancelCurrentCustomerOrder("MC-13");

        verify(vnpayProcessor).checkCanCancel(eq(13L), any());
        verify(inventoryService).restoreForCancellation(any());
    }

    @Test
    void customerCannotCancelPaidVnpayOrder() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(8L);
        SalesOrder order = order(14L, OrderStatus.PLACED);
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(customer);
        when(orderRepository.findOwnedByOrderCode("MC-14", 8L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(14L))
            .thenReturn(List.of(payment(PaymentMethod.VNPAY, PaymentStatus.PAID)));

        assertThatThrownBy(() -> service.cancelCurrentCustomerOrder("MC-14"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("hoàn tiền");

        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void adminCancelsPaidVnpayProcessingOrderAndPaymentStaysPaid() {
        SalesOrder order = order(15L, OrderStatus.PROCESSING);
        Payment paid = payment(PaymentMethod.VNPAY, PaymentStatus.PAID);
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(15L)).thenReturn(List.of(paid));
        when(orderRepository.cancelFromStatus(
            eq(15L), eq(OrderStatus.PROCESSING), eq(OrderStatus.CANCELLED), any()
        )).thenReturn(1);
        OrderItem standard = item(151L, InventorySource.STOCKED_SKU, 1);
        standard.setVariant(variant(30L));
        when(itemRepository.findByOrderId(15L)).thenReturn(List.of(standard));

        service.cancelForAdmin(15L);

        verify(inventoryService).restoreForCancellation(any());
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
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

    @Test
    void adminCannotCancelDeliveredOrder() {
        SalesOrder order = order(41L, OrderStatus.DELIVERED);
        when(orderRepository.findById(41L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelForAdmin(41L))
            .isInstanceOf(BusinessRuleViolationException.class);

        verify(orderRepository, never()).cancelFromStatus(
            anyLong(),
            any(),
            any(),
            any()
        );
        verifyNoInteractions(inventoryService);
    }

    @Test
    void customerCannotCancelDeliveredOrder() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(8L);
        SalesOrder order = order(42L, OrderStatus.DELIVERED);
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(customer);
        when(orderRepository.findOwnedByOrderCode("MC-42", 8L))
            .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelCurrentCustomerOrder("MC-42"))
            .isInstanceOf(BusinessRuleViolationException.class);

        verify(orderRepository, never()).cancelFromStatus(
            anyLong(),
            any(),
            any(),
            any()
        );
        verifyNoInteractions(inventoryService);
    }

    @Test
    void expiredUnpaidVnpayOrderIsCancelledAndStockReturned() {
        SalesOrder order = order(20L, OrderStatus.PLACED);
        Payment payment = payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(20L)).thenReturn(List.of(payment));
        when(vnpayProcessor.autoCancelAt(order, payment))
            .thenReturn(Optional.of(LocalDateTime.of(2026, 10, 22, 9, 59)));
        when(orderRepository.cancelFromStatus(
            eq(20L), eq(OrderStatus.PLACED), eq(OrderStatus.CANCELLED), any()
        )).thenReturn(1);
        OrderItem standard = item(201L, InventorySource.STOCKED_SKU, 1);
        standard.setVariant(variant(30L));
        when(itemRepository.findByOrderId(20L)).thenReturn(List.of(standard));

        boolean cancelled = service.cancelIfPaymentExpired(20L);

        assertThat(cancelled).isTrue();
        InOrder inOrder = inOrder(vnpayProcessor, orderRepository, inventoryService);
        inOrder.verify(vnpayProcessor).refreshPaymentStatus(20L);
        inOrder.verify(orderRepository).cancelFromStatus(eq(20L), any(), any(), any());
        inOrder.verify(inventoryService).restoreForCancellation(any());
    }

    @Test
    void unpaidVnpayOrderBeforeDeadlineIsKept() {
        SalesOrder order = order(21L, OrderStatus.PLACED);
        Payment payment = payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID);
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(21L)).thenReturn(List.of(payment));
        when(vnpayProcessor.autoCancelAt(order, payment))
            .thenReturn(Optional.of(LocalDateTime.of(2026, 10, 22, 10, 5)));

        boolean cancelled = service.cancelIfPaymentExpired(21L);

        assertThat(cancelled).isFalse();
        verify(vnpayProcessor, never()).refreshPaymentStatus(anyLong());
        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
    }

    @Test
    void orderPaidAtVnpayDuringCheckIsKept() {
        SalesOrder order = order(22L, OrderStatus.PLACED);
        Payment payment = payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID);
        when(orderRepository.findById(22L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(22L)).thenReturn(List.of(payment));
        when(vnpayProcessor.autoCancelAt(order, payment))
            .thenReturn(Optional.of(LocalDateTime.of(2026, 10, 22, 9, 0)));
        doAnswer(invocation -> {
            payment.setStatus(PaymentStatus.PAID);
            return null;
        }).when(vnpayProcessor).refreshPaymentStatus(22L);

        boolean cancelled = service.cancelIfPaymentExpired(22L);

        assertThat(cancelled).isFalse();
        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void vnpayUnreachableLeavesOrderForNextRun() {
        SalesOrder order = order(23L, OrderStatus.PLACED);
        Payment payment = payment(PaymentMethod.VNPAY, PaymentStatus.UNPAID);
        when(orderRepository.findById(23L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(23L)).thenReturn(List.of(payment));
        when(vnpayProcessor.autoCancelAt(order, payment))
            .thenReturn(Optional.of(LocalDateTime.of(2026, 10, 22, 9, 0)));
        doThrow(new BusinessRuleViolationException("offline"))
            .when(vnpayProcessor).refreshPaymentStatus(23L);

        assertThatThrownBy(() -> service.cancelIfPaymentExpired(23L))
            .isInstanceOf(BusinessRuleViolationException.class);
        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
    }

    @Test
    void customerViewOfAnotherCustomersOrderDoesNothing() {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(ShopperIdentity.forCustomer(8L));
        when(orderRepository.findOwnedByOrderCode("MC-OTHER", 8L)).thenReturn(Optional.empty());

        boolean cancelled = service.cancelOwnOrderIfPaymentExpired("MC-OTHER");

        assertThat(cancelled).isFalse();
        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
    }

    @Test
    void codOrderIsNeverAutoCancelled() {
        SalesOrder order = order(24L, OrderStatus.PLACED);
        when(orderRepository.findById(24L)).thenReturn(Optional.of(order));

        boolean cancelled = service.cancelIfPaymentExpired(24L);

        assertThat(cancelled).isFalse();
        verify(orderRepository, never()).findByIdForUpdate(anyLong());
        verify(orderRepository, never()).cancelFromStatus(anyLong(), any(), any(), any());
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

    private static PaymentProcessor vnpayProcessorMock() {
        PaymentProcessor processor = mock(PaymentProcessor.class);
        when(processor.method()).thenReturn(PaymentMethod.VNPAY);
        return processor;
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
