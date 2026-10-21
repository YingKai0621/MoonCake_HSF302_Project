package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.OrderItemComponent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemComponentRepository
    extends Repository<OrderItemComponent, Long> {

    OrderItemComponent save(OrderItemComponent component);

    @Query("""
        select component
        from OrderItemComponent component
        join fetch component.orderItem item
        where item.id in :orderItemIds
        order by item.id, component.id
        """)
    List<OrderItemComponent> findByOrderItemIds(
        @Param("orderItemIds") Collection<Long> orderItemIds
    );
}
