package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.enums.OrderStatus;

import java.util.List;

public record AdminOrderListPageView(
    List<AdminOrderView> orders,
    List<AdminOrderTab> tabs,
    OrderStatus status,
    int pageNumber
) {
    public AdminOrderListPageView {
        orders = List.copyOf(orders);
        tabs = List.copyOf(tabs);
    }
}
