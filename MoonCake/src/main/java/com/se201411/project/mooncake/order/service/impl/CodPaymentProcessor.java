package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.PaymentReturnResult;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CodPaymentProcessor implements PaymentProcessor {
    private static final BigDecimal MAX_COD_TOTAL = BigDecimal.valueOf(5_000_000);
    private static final int MAX_OPEN_COD_ORDERS = 2;

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
    public String displayName() {
        return "COD";
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public Optional<String> startPayment(SalesOrder order, String clientIp) {
        return Optional.empty();
    }

    @Override
    public Optional<LocalDateTime> autoCancelAt(SalesOrder order, Payment payment) {
        return Optional.empty();
    }

    @Override
    public void checkCanCancel(long orderId, Payment payment) {
    }

    @Override
    public void checkCanFulfil(Payment payment) {
    }

    @Override
    public boolean canFulfil(Payment payment) {
        return true;
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
    public PaymentReturnResult handleReturn(Map<String, String> params) {
        return PaymentReturnResult.unverified();
    }

    @Override
    public boolean acceptsTotal(BigDecimal total) {
        return total.compareTo(MAX_COD_TOTAL) <= 0;
    }

    @Override
    public void checkCanPlace(long customerId, BigDecimal total) {
        if (!acceptsTotal(total)) {
            throw new BusinessRuleViolationException(
                "COD chỉ áp dụng cho đơn đến 5.000.000đ. Đơn lớn hơn vui lòng thanh toán qua VNPay."
            );
        }
        long openOrders = paymentRepository.countCustomerOrders(
            customerId,
            OrderStatus.PLACED,
            PaymentMethod.COD,
            PaymentStatus.UNPAID
        );
        if (openOrders >= MAX_OPEN_COD_ORDERS) {
            throw new BusinessRuleViolationException(
                "Bạn đang có " + openOrders + " đơn COD chờ xử lý. Vui lòng chờ cửa hàng xử lý hoặc hủy bớt đơn trước khi đặt thêm."
            );
        }
    }
}
