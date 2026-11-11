package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.CustomerOpenOrderView;
import com.se201411.project.mooncake.order.repository.OrderStatisticsRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.impl.OrderStatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderStatisticsServiceImplTest {

    private final OrderStatisticsRepository repository = mock(OrderStatisticsRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final PaymentProcessorRegistry registry = mock(PaymentProcessorRegistry.class);
    private final OrderStatisticsService service = new OrderStatisticsServiceImpl(repository, payments, registry);

    @Test
    void topProductsCountDeliveredAndCompletedOrders() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);

        service.topCompletedProducts(from, 5);

        verify(repository).findTopProducts(
            from,
            List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED),
            PageRequest.of(0, 5)
        );
    }

    @Test
    void refundTaskCountsPaidPaymentsOfCancelledOrders() {
        service.countPaidCancelledOrders();

        verify(repository).countPaidCancelledOrders(OrderStatus.CANCELLED, PaymentStatus.PAID);
    }

    @Test
    void openOrderIsCancellableOnlyWhenPlacedAndUnpaid() {
        SalesOrder placedUnpaid = order(1L, "MC-1", OrderStatus.PLACED);
        SalesOrder placedPaid = order(2L, "MC-2", OrderStatus.PLACED);
        SalesOrder shippingUnpaid = order(3L, "MC-3", OrderStatus.SHIPPING);
        when(repository.findByCustomer_IdAndStatusInOrderByCreatedAtDesc(anyLong(), anyCollection()))
            .thenReturn(List.of(placedUnpaid, placedPaid, shippingUnpaid));
        when(payments.findByOrderIds(anyList())).thenReturn(List.of(
            payment(placedUnpaid, PaymentMethod.COD, PaymentStatus.UNPAID),
            payment(placedPaid, PaymentMethod.VNPAY, PaymentStatus.PAID),
            payment(shippingUnpaid, PaymentMethod.COD, PaymentStatus.UNPAID)
        ));
        PaymentProcessor cod = mock(PaymentProcessor.class);
        PaymentProcessor vnpay = mock(PaymentProcessor.class);
        when(cod.displayName()).thenReturn("COD");
        when(vnpay.displayName()).thenReturn("VNPay");
        when(registry.forMethod(PaymentMethod.COD)).thenReturn(cod);
        when(registry.forMethod(PaymentMethod.VNPAY)).thenReturn(vnpay);

        List<CustomerOpenOrderView> views = service.openOrdersOf(7L);

        assertThat(views).extracting(CustomerOpenOrderView::orderCode)
            .containsExactly("MC-1", "MC-2", "MC-3");
        assertThat(views).extracting(CustomerOpenOrderView::cancellableOnLock)
            .containsExactly(true, false, false);
        assertThat(views.get(0).paymentLabel()).isEqualTo("COD · Chưa thanh toán");
        assertThat(views.get(1).paymentLabel()).isEqualTo("VNPay · Đã thanh toán");
        verify(repository).findByCustomer_IdAndStatusInOrderByCreatedAtDesc(
            7L,
            List.of(OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.SHIPPING)
        );
    }

    @Test
    void orderCountsWithEmptyInputSkipDatabase() {
        assertThat(service.orderCountsByCustomer(List.of())).isEmpty();

        verifyNoInteractions(repository);
    }

    private static SalesOrder order(long id, String code, OrderStatus status) {
        SalesOrder order = new SalesOrder();
        ReflectionTestUtils.setField(order, "id", id);
        order.setOrderCode(code);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("250000"));
        return order;
    }

    private static Payment payment(SalesOrder order, PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setStatus(status);
        return payment;
    }
}
