package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.order.dto.PaidAmount;
import com.se201411.project.mooncake.order.dto.ProductSalesSummary;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderStatisticsService {

    List<SalesOrder> ordersCreatedSince(LocalDateTime from);

    List<PaidAmount> paymentsPaidSince(LocalDateTime from);

    long countPlacedOrders();

    long countPaidCancelledOrders();

    List<SalesOrder> latestOrders();

    List<SalesOrder> latestDiscountedOrders();

    List<ProductSalesSummary> topCompletedProducts(LocalDateTime from, int limit);
}
