package com.se201411.project.mooncake.order.repository;

import java.time.LocalDate;
import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderAdminRepository {

    List<SalesOrder> orders(OrderStatus status, int page);

    long count(OrderStatus status);

    SalesOrder require(long id);

    SalesOrder requireForUpdate(long id);

    int transition(long id, OrderStatus from, OrderStatus to, LocalDateTime now);

    int ship(long id, ShippingCarrier carrier, String trackingCode, LocalDate estimatedDeliveryDate, LocalDateTime now);
}
