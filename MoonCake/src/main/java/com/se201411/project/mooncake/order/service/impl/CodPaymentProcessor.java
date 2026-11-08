package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<String> startPayment(SalesOrder order, String clientIp) {
        return Optional.empty();
    }

    @Override
    public void checkCanCancel(long orderId, Payment payment) {
    }

    @Override
    public void checkCanFulfil(Payment payment) {
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

    @Override
    public void refreshPaymentStatus(long orderId) {
    }

    @Override
    public Map<String, String> handleNotification(Map<String, String> params) {
        throw new UnsupportedOperationException("COD has no online payment notification");
    }

    @Override
    public boolean verifyReturn(Map<String, String> params) {
        return false;
    }
}
