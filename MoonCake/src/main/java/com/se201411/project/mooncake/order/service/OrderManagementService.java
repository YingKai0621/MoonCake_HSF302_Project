package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.AdminOrderView;
import com.se201411.project.mooncake.enums.OrderStatus;

import java.util.List;

public interface OrderManagementService {
    List<AdminOrderView> orders(OrderStatus status, int page);

    AdminOrderView detail(long id);

    void transition(long id, OrderStatus target);
}
