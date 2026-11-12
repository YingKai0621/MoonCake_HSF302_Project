package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.impl.DeliveredOrderCompletionServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveredOrderCompletionServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-11-12T10:00:00Z"),
        ZoneOffset.UTC
    );

    private final SalesOrderRepository orderRepository =
        mock(SalesOrderRepository.class);
    private final DeliveredOrderCompletionService service =
        new DeliveredOrderCompletionServiceImpl(orderRepository, CLOCK);

    @Test
    void completesOnlyOrdersDeliveredMoreThanThreeDaysAgo() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 11, 9, 10, 0);
        LocalDateTime now = LocalDateTime.of(2026, 11, 12, 10, 0);
        when(orderRepository.findDeliveredBefore(OrderStatus.DELIVERED, cutoff))
            .thenReturn(List.of(11L));
        when(orderRepository.autoComplete(
            11L,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED,
            now
        )).thenReturn(1);

        int completed = service.completeOverdueDeliveries();

        assertThat(completed).isEqualTo(1);
        verify(orderRepository).findDeliveredBefore(OrderStatus.DELIVERED, cutoff);
    }

    @Test
    void countsOnlyRowsActuallyChanged() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 11, 9, 10, 0);
        LocalDateTime now = LocalDateTime.of(2026, 11, 12, 10, 0);
        when(orderRepository.findDeliveredBefore(OrderStatus.DELIVERED, cutoff))
            .thenReturn(List.of(11L, 12L));
        when(orderRepository.autoComplete(
            11L,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED,
            now
        )).thenReturn(1);
        when(orderRepository.autoComplete(
            12L,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED,
            now
        )).thenReturn(0);

        assertThat(service.completeOverdueDeliveries()).isEqualTo(1);
    }
}
