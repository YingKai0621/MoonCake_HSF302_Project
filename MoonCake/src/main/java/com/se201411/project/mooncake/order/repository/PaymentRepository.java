package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment save(Payment payment);

    @Query("""
        select payment
        from Payment payment
        where payment.order.id = :orderId
        order by payment.id
        """)
    List<Payment> findByOrderId(@Param("orderId") long orderId);

    @Query("""
        select payment
        from Payment payment
        where payment.order.id in :orderIds
        """)
    List<Payment> findByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Query("""
        select payment.order.id
        from Payment payment
        where payment.order.status = :orderStatus
          and payment.status = :paymentStatus
        order by payment.order.createdAt
        """)
    List<Long> findOrderIdsByStatus(
        @Param("orderStatus") OrderStatus orderStatus,
        @Param("paymentStatus") PaymentStatus paymentStatus
    );

    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
        UPDATE dbo.PAYMENT
        SET payment_status = 'PAID', paid_at = :now, updated_at = :now
        WHERE order_id = :orderId
          AND payment_method = 'COD'
          AND payment_status = 'UNPAID'
          AND amount = (SELECT total_amount FROM dbo.SALES_ORDER WHERE order_id = :orderId)
          AND (SELECT COUNT(*) FROM dbo.PAYMENT WHERE order_id = :orderId) = 1
        """)
    int collectCashOnDelivery(
        @Param("orderId") long orderId,
        @Param("now") LocalDateTime now
    );

    @Query("""
        select count(payment)
        from Payment payment
        where payment.order.customer.id = :customerId
          and payment.order.status = :orderStatus
          and payment.method = :method
          and payment.status = :paymentStatus
        """)
    long countCustomerOrders(
        @Param("customerId") long customerId,
        @Param("orderStatus") OrderStatus orderStatus,
        @Param("method") PaymentMethod method,
        @Param("paymentStatus") PaymentStatus paymentStatus
    );

    @Query("""
        select count(payment)
        from Payment payment
        where payment.order.customer.id = :customerId
          and payment.order.status = :cancelled
          and payment.method = :method
          and payment.status = :unpaid
          and payment.order.updatedAt >= :since
        """)
    long countCancelledUnpaidSince(
        @Param("customerId") long customerId,
        @Param("cancelled") OrderStatus cancelled,
        @Param("method") PaymentMethod method,
        @Param("unpaid") PaymentStatus unpaid,
        @Param("since") LocalDateTime since
    );
}
