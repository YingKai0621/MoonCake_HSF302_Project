package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.PaidAmount;
import com.se201411.project.mooncake.order.dto.ProductSalesSummary;
import com.se201411.project.mooncake.order.repository.OrderStatisticsRepository;
import com.se201411.project.mooncake.order.service.OrderStatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderStatisticsServiceImpl implements OrderStatisticsService {

    private final OrderStatisticsRepository statistics;

    public OrderStatisticsServiceImpl(OrderStatisticsRepository statistics) {
        this.statistics = statistics;
    }

    @Override
    public List<SalesOrder> ordersCreatedSince(LocalDateTime from) {
        return statistics.findByCreatedAtGreaterThanEqual(from);
    }

    @Override
    public List<PaidAmount> paymentsPaidSince(LocalDateTime from) {
        return statistics.findPaidSince(PaymentStatus.PAID, from);
    }

    @Override
    public long countPlacedOrders() {
        return statistics.countByStatus(OrderStatus.PLACED);
    }

    @Override
    public long countPaidCancelledOrders() {
        return statistics.countPaidCancelledOrders(OrderStatus.CANCELLED, PaymentStatus.PAID);
    }

    @Override
    public List<SalesOrder> latestOrders() {
        return statistics.findTop5ByOrderByCreatedAtDesc();
    }

    @Override
    public List<SalesOrder> latestDiscountedOrders() {
        return statistics.findTop5ByDiscountAmountGreaterThanAndStatusNotOrderByCreatedAtDesc(
            BigDecimal.ZERO,
            OrderStatus.CANCELLED
        );
    }

    @Override
    public List<ProductSalesSummary> topCompletedProducts(LocalDateTime from, int limit) {
        return statistics.findTopProducts(
            from,
            List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED),
            PageRequest.of(0, limit)
        );
    }
}
