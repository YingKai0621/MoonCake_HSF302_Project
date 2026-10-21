package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.SalesOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalesOrderRepository extends Repository<SalesOrder, Long> {

    SalesOrder save(SalesOrder order);

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
}
