package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/** Cash on delivery: nothing to do at checkout, cash is collected on delivery. */
@Service
public class CodPaymentProcessor implements PaymentProcessor {

    private final PaymentRepository paymentRepository;

    public CodPaymentProcessor(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public Optional<String> startPayment(SalesOrder order, String clientIp) {
        return Optional.empty();
    }

    @Override
    public void checkCanCancel(Payment payment) {
        // Nothing has been paid yet, so a COD order can always be cancelled
        // (the order status rules are checked by the cancellation service).
    }

    @Override
    public void checkCanFulfil(Payment payment) {
        // COD orders are processed before the money is collected.
    }

    @Override
    public void completeDelivery(long orderId, Payment payment, LocalDateTime now) {
        int changedRows = paymentRepository.collectCashOnDelivery(orderId, now);
        if (changedRows != 1) {
            throw new BusinessRuleViolationException(
                "Không thể xác nhận COD. Đơn phải có đúng một thanh toán COD chưa thu, đúng số tiền."
            );
        }
    }
}
