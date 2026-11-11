package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.order.dto.CustomerOpenOrderView;

import java.util.List;

public record AdminCustomerDetailPageView(
    AdminCustomerRow customer,
    List<CustomerOpenOrderView> openOrders,
    int cancellableCount
) {

    public AdminCustomerDetailPageView {
        openOrders = List.copyOf(openOrders);
    }
}
