package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Query("select count(distinct item.order.id) from OrderItem item where item.variant.id in :variantIds")
    long countOrdersByVariantIds(@Param("variantIds") Collection<Long> variantIds);

    @Query("select count(item) from OrderItem item where item.variant.id in :variantIds")
    long countByVariantIds(@Param("variantIds") Collection<Long> variantIds);

    @Query("select count(item) from OrderItem item where item.configuration.id in :configurationIds")
    long countByConfigurationIds(@Param("configurationIds") Collection<Long> configurationIds);
}
