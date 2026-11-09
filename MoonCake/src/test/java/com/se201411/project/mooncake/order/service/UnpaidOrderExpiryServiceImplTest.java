package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.impl.UnpaidOrderExpiryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnpaidOrderExpiryServiceImplTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final OrderCancellationService orderCancellationService = mock(OrderCancellationService.class);
    private final UnpaidOrderExpiryService service = new UnpaidOrderExpiryServiceImpl(
        paymentRepository,
        orderCancellationService
    );

    @Test
    void checksEveryUnpaidPlacedOrderAndCountsCancellations() {
        when(paymentRepository.findOrderIdsByStatus(OrderStatus.PLACED, PaymentStatus.UNPAID))
            .thenReturn(List.of(1L, 2L, 3L));
        when(orderCancellationService.cancelIfPaymentExpired(1L)).thenReturn(true);
        when(orderCancellationService.cancelIfPaymentExpired(2L)).thenReturn(false);
        when(orderCancellationService.cancelIfPaymentExpired(3L)).thenReturn(true);

        int cancelled = service.cancelExpiredUnpaidOrders();

        assertThat(cancelled).isEqualTo(2);
    }

    @Test
    void oneFailingOrderDoesNotStopTheOthers() {
        when(paymentRepository.findOrderIdsByStatus(OrderStatus.PLACED, PaymentStatus.UNPAID))
            .thenReturn(List.of(1L, 2L));
        when(orderCancellationService.cancelIfPaymentExpired(1L))
            .thenThrow(new BusinessRuleViolationException("offline"));
        when(orderCancellationService.cancelIfPaymentExpired(2L)).thenReturn(true);

        int cancelled = service.cancelExpiredUnpaidOrders();

        assertThat(cancelled).isEqualTo(1);
        verify(orderCancellationService).cancelIfPaymentExpired(2L);
    }
}
