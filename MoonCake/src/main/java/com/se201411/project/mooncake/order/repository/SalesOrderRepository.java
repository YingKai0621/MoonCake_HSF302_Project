package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    SalesOrder save(SalesOrder order);

    Optional<SalesOrder> findById(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select salesOrder from SalesOrder salesOrder where salesOrder.id = :orderId")
    Optional<SalesOrder> findByIdForUpdate(@Param("orderId") long orderId);

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
            salesOrder.updatedAt = :updatedAt,
            salesOrder.cancelledAt = :updatedAt
        where salesOrder.id = :orderId
          and salesOrder.status = :expectedStatus
        """)
    int cancelFromStatus(
        @Param("orderId") long orderId,
        @Param("expectedStatus") OrderStatus expectedStatus,
        @Param("cancelledStatus") OrderStatus cancelledStatus,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SalesOrder salesOrder
        set salesOrder.recipientName = :recipientName,
            salesOrder.recipientPhone = :recipientPhone,
            salesOrder.shippingAddressSnapshot = :shippingAddress,
            salesOrder.orderNote = :orderNote,
            salesOrder.updatedAt = :updatedAt
        where salesOrder.id = :orderId
          and salesOrder.customer.id = :customerId
          and salesOrder.status = :expectedStatus
        """)
    int updateDeliveryIfStatus(
        @Param("orderId") long orderId,
        @Param("customerId") long customerId,
        @Param("expectedStatus") OrderStatus expectedStatus,
        @Param("recipientName") String recipientName,
        @Param("recipientPhone") String recipientPhone,
        @Param("shippingAddress") String shippingAddress,
        @Param("orderNote") String orderNote,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SalesOrder salesOrder
        set salesOrder.status = :completed,
            salesOrder.completedAt = :now,
            salesOrder.customerConfirmedAt = :now,
            salesOrder.updatedAt = :now
        where salesOrder.id = :orderId
          and salesOrder.status = :delivered
        """)
    int confirmReceived(
        @Param("orderId") long orderId,
        @Param("delivered") OrderStatus delivered,
        @Param("completed") OrderStatus completed,
        @Param("now") LocalDateTime now
    );

    @Query("""
        select salesOrder.id
        from SalesOrder salesOrder
        where salesOrder.status = :delivered
          and salesOrder.deliveredAt <= :cutoff
        """)
    List<Long> findDeliveredBefore(
        @Param("delivered") OrderStatus delivered,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SalesOrder salesOrder
        set salesOrder.status = :completed,
            salesOrder.completedAt = :now,
            salesOrder.updatedAt = :now
        where salesOrder.id = :orderId
          and salesOrder.status = :delivered
        """)
    int autoComplete(
        @Param("orderId") long orderId,
        @Param("delivered") OrderStatus delivered,
        @Param("completed") OrderStatus completed,
        @Param("now") LocalDateTime now
    );
}
