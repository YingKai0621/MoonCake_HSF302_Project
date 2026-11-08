package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.Payment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends Repository<Payment, Long> {

    Payment save(Payment payment);

    @Query("""
        select payment
        from Payment payment
        where payment.order.id = :orderId
        order by payment.id
        """)
    List<Payment> findByOrderId(@Param("orderId") long orderId);

    /**
     * Marks the order's single COD payment as PAID when the cash is collected.
     * Returns 1 only when the order has exactly one UNPAID COD payment whose
     * amount equals the order total; repeated calls return 0.
     */
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
}
