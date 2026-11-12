package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.DeliveredOrderCompletionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveredOrderCompletionServiceImpl
    implements DeliveredOrderCompletionService {

    private static final int AUTO_COMPLETE_DAYS = 3;

    private final SalesOrderRepository orderRepository;
    private final Clock clock;

    public DeliveredOrderCompletionServiceImpl(
        SalesOrderRepository orderRepository,
        Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int completeOverdueDeliveries() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(AUTO_COMPLETE_DAYS);
        List<Long> orderIds = orderRepository.findDeliveredBefore(
            OrderStatus.DELIVERED,
            cutoff
        );
        int completed = 0;
        for (Long orderId : orderIds) {
            completed += orderRepository.autoComplete(
                orderId,
                OrderStatus.DELIVERED,
                OrderStatus.COMPLETED,
                now
            );
        }
        return completed;
    }
}
