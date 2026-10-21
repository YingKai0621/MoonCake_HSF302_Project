package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

import java.util.List;

public interface CustomerOrderService {

    List<CustomerOrderSummaryView> findOrders(ShopperIdentity customerIdentity);

    CustomerOrderDetailView requireOrder(
        String orderCode,
        ShopperIdentity customerIdentity
    );
}
