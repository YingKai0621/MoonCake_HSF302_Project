package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.CustomerOpenOrderView;
import com.se201411.project.mooncake.order.dto.CustomerOrderCount;
import com.se201411.project.mooncake.order.dto.PaidAmount;
import com.se201411.project.mooncake.order.dto.ProductSalesSummary;
import com.se201411.project.mooncake.order.repository.OrderStatisticsRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.OrderStatisticsService;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderStatisticsServiceImpl implements OrderStatisticsService {

    private static final List<OrderStatus> OPEN_STATUSES = List.of(
        OrderStatus.PLACED,
        OrderStatus.PROCESSING,
        OrderStatus.SHIPPING
    );

    private final OrderStatisticsRepository statistics;
    private final PaymentRepository payments;
    private final PaymentProcessorRegistry paymentProcessorRegistry;

    public OrderStatisticsServiceImpl(
        OrderStatisticsRepository statistics,
        PaymentRepository payments,
        PaymentProcessorRegistry paymentProcessorRegistry
    ) {
        this.statistics = statistics;
        this.payments = payments;
        this.paymentProcessorRegistry = paymentProcessorRegistry;
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
        return statistics.findTopProducts(from, OrderStatus.COMPLETED, PageRequest.of(0, limit));
    }

    @Override
    public Map<Long, CustomerOrderCount> orderCountsByCustomer(Collection<Long> customerIds) {
        Map<Long, CustomerOrderCount> result = new HashMap<>();
        if (customerIds.isEmpty()) {
            return result;
        }
        for (CustomerOrderCount count : statistics.countOrdersByCustomer(customerIds, OrderStatus.CANCELLED)) {
            result.put(count.customerId(), count);
        }
        return result;
    }

    @Override
    public List<CustomerOpenOrderView> openOrdersOf(long customerId) {
        List<SalesOrder> openOrders = statistics.findByCustomer_IdAndStatusInOrderByCreatedAtDesc(
            customerId,
            OPEN_STATUSES
        );
        if (openOrders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = new ArrayList<>();
        for (SalesOrder order : openOrders) {
            orderIds.add(order.getId());
        }
        Map<Long, Payment> paymentByOrderId = new HashMap<>();
        for (Payment payment : payments.findByOrderIds(orderIds)) {
            paymentByOrderId.put(payment.getOrder().getId(), payment);
        }

        List<CustomerOpenOrderView> views = new ArrayList<>();
        for (SalesOrder order : openOrders) {
            Payment payment = paymentByOrderId.get(order.getId());
            if (payment == null) {
                throw new IllegalStateException("Đơn " + order.getOrderCode() + " không có thông tin thanh toán");
            }
            String methodLabel = paymentProcessorRegistry.forMethod(payment.getMethod()).displayName();
            String statusLabel = openStatusLabel(order.getStatus());
            views.add(CustomerOpenOrderView.entityToDto(order, payment, statusLabel, methodLabel));
        }
        return views;
    }

    private String openStatusLabel(OrderStatus status) {
        if (status == OrderStatus.PLACED) {
            return "Chờ xử lý";
        }
        if (status == OrderStatus.PROCESSING) {
            return "Đang chuẩn bị";
        }
        return "Đang giao";
    }
}
