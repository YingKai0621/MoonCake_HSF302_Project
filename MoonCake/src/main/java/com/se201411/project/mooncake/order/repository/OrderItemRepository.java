package com.se201411.project.mooncake.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.OrderItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    OrderItem save(OrderItem item);

    @Query("""
        select item
        from OrderItem item
        where item.order.id = :orderId
        order by item.id
        """)
    List<OrderItem> findByOrderId(@Param("orderId") long orderId);
}
