package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/** VNPay Sandbox: the customer pays online before the shop processes the order. */
@Service
public class VnpayPaymentProcessor implements PaymentProcessor {

    private final VnpayPaymentService vnpayPaymentService;

    public VnpayPaymentProcessor(VnpayPaymentService vnpayPaymentService) {
        this.vnpayPaymentService = vnpayPaymentService;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public Optional<String> startPayment(SalesOrder order, String clientIp) {
        String paymentUrl = vnpayPaymentService.startPayment(order.getOrderCode(), clientIp);
        return Optional.of(paymentUrl);
    }

    @Override
    public void checkCanCancel(Payment payment) {
        throw new BusinessRuleViolationException(
            "Đơn đã mở thanh toán VNPay cần liên hệ cửa hàng để đối soát/hoàn tiền; không thể tự hủy."
        );
    }

    @Override
    public void checkCanFulfil(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessRuleViolationException(
                "VNPay chưa xác nhận thanh toán, chưa thể xử lý đơn."
            );
        }
    }

    @Override
    public void completeDelivery(long orderId, Payment payment, LocalDateTime now) {
        // The money was already collected online; there is no cash to collect.
        checkCanFulfil(payment);
    }
}
