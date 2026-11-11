package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.dto.AdminOrderListPageView;
import com.se201411.project.mooncake.order.dto.AdminOrderTab;
import com.se201411.project.mooncake.order.dto.AdminOrderView;

import java.util.Collection;
import java.util.List;

public interface OrderManagementService {
    List<AdminOrderView> orders(OrderStatus status, int page);

    List<AdminOrderTab> statusTabs(OrderStatus selected);

    AdminOrderView detail(long id);

    void transition(long id, OrderStatus target);

    void refreshPaymentStatus(long id);

    long countOrdersContaining(Collection<Long> variantIds);

    long countItemsWithVariants(Collection<Long> variantIds);

    long countItemsWithConfigurations(Collection<Long> configurationIds);

    AdminOrderListPageView ordersPage(OrderStatus status, int page);
}
