package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.order.service.UnpaidOrderExpiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnpaidOrderExpiryServiceImpl implements UnpaidOrderExpiryService {

    private static final Logger LOG = LoggerFactory.getLogger(UnpaidOrderExpiryServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderCancellationService orderCancellationService;

    public UnpaidOrderExpiryServiceImpl(
        PaymentRepository paymentRepository,
        OrderCancellationService orderCancellationService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderCancellationService = orderCancellationService;
    }

    @Override
    public int cancelExpiredUnpaidOrders() {
        List<Long> orderIds = paymentRepository.findOrderIdsByStatus(
            OrderStatus.PLACED,
            PaymentStatus.UNPAID
        );

        int cancelledCount = 0;
        for (Long orderId : orderIds) {
            try {
                if (orderCancellationService.cancelIfPaymentExpired(orderId)) {
                    cancelledCount++;
                }
            } catch (RuntimeException exception) {
                LOG.warn("Could not auto-cancel order {}; will retry next run: {}", orderId, exception.getMessage());
            }
        }
        return cancelledCount;
    }
}
