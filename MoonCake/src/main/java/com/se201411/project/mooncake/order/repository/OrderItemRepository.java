package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.OrderItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends Repository<OrderItem, Long> {

    OrderItem save(OrderItem item);

    @Query("""
        select item
        from OrderItem item
        where item.order.id = :orderId
        order by item.id
        """)
    List<OrderItem> findByOrderId(@Param("orderId") long orderId);
}
