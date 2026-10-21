package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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
}
