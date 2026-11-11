package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.repository.OrderStatisticsRepository;
import com.se201411.project.mooncake.order.service.impl.OrderStatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderStatisticsServiceImplTest {

    private final OrderStatisticsRepository repository = mock(OrderStatisticsRepository.class);
    private final OrderStatisticsService service = new OrderStatisticsServiceImpl(repository);

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
}
