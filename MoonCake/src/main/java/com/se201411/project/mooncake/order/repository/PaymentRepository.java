package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.Payment;
import org.springframework.data.repository.Repository;

public interface PaymentRepository extends Repository<Payment, Long> {

    Payment save(Payment payment);
}
