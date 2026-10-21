package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.OrderItemComponent;
import org.springframework.data.repository.Repository;

public interface OrderItemComponentRepository
    extends Repository<OrderItemComponent, Long> {

    OrderItemComponent save(OrderItemComponent component);
}
