package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends Repository<SalesOrder, Long> {

    SalesOrder save(SalesOrder order);

    Optional<SalesOrder> findById(Long orderId);

    @Query("""
        select salesOrder
        from SalesOrder salesOrder
        where salesOrder.customer.id = :customerId
        order by salesOrder.createdAt desc, salesOrder.id desc
        """)
    List<SalesOrder> findAllOwnedByCustomerId(
        @Param("customerId") long customerId
    );

    @Query("""
        select salesOrder
        from SalesOrder salesOrder
        where salesOrder.orderCode = :orderCode
          and salesOrder.customer.id = :customerId
        """)
    Optional<SalesOrder> findOwnedByOrderCode(
        @Param("orderCode") String orderCode,
        @Param("customerId") long customerId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SalesOrder salesOrder
        set salesOrder.status = :cancelledStatus,
            salesOrder.updatedAt = :updatedAt
        where salesOrder.id = :orderId
          and salesOrder.status = :expectedStatus
        """)
    int cancelFromStatus(
        @Param("orderId") long orderId,
        @Param("expectedStatus") OrderStatus expectedStatus,
        @Param("cancelledStatus") OrderStatus cancelledStatus,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}
