package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.VnpayPendingAttempt;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VnpayPaymentServiceImpl implements VnpayPaymentService {

    /** A VNPay attempt stays valid for 15 minutes. */
    private static final long ATTEMPT_LIFETIME_SECONDS = 900;

    // VNPay IPN response codes.
    private static final String RSP_SUCCESS = "00";
    private static final String RSP_ORDER_NOT_FOUND = "01";
    private static final String RSP_ALREADY_CONFIRMED = "02";
    private static final String RSP_INVALID_AMOUNT = "04";
    private static final String RSP_INVALID_SIGNATURE = "97";
    private static final String RSP_OTHER = "99";

    private static final String VNPAY_SUCCESS = "00";
    private static final String VNPAY_TRANSACTION_FAILED = "02";
    private static final String VNPAY_PAYMENT_TRANSACTION = "01";

    private final VnpayRepository vnpayRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentCustomerService currentCustomerService;
    private final VnpayGateway gateway;
    private final Clock clock;

    public VnpayPaymentServiceImpl(
        VnpayRepository vnpayRepository,
        PaymentRepository paymentRepository,
        CurrentCustomerService currentCustomerService,
        VnpayGateway gateway,
        Clock clock
    ) {
        this.vnpayRepository = vnpayRepository;
        this.paymentRepository = paymentRepository;
        this.currentCustomerService = currentCustomerService;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public boolean isEnabled() {
        return gateway.enabled();
    }

    @Override
    @Transactional
    public String startPayment(String orderCode, String clientIp) {
        gateway.requireConfigured();
        long customerId = currentCustomerService.requireCurrentCustomer().customerId();
        SalesOrder order = vnpayRepository.lockOwned(orderCode, customerId);
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());

        boolean canPayOnline = order.getStatus() == OrderStatus.PLACED
            && payments.size() == 1
            && payments.get(0).getStatus() == PaymentStatus.UNPAID
            && order.getTotalAmount().signum() > 0;
        if (!canPayOnline) {
            throw new BusinessRuleViolationException(
                "Chỉ thanh toán online cho đơn chờ xử lý, chưa trả tiền."
            );
        }
        // Business Rules v1.7: the payment method is fixed at checkout.
        Payment payment = payments.get(0);
        if (payment.getMethod() != PaymentMethod.VNPAY) {
            throw new BusinessRuleViolationException(
                "Đơn này thanh toán khi nhận hàng (COD) nên không thể chuyển sang VNPay. "
                    + "Muốn thanh toán online, vui lòng hủy đơn và đặt lại."
            );
        }

        VnpayRepository.Pending pending = vnpayRepository.pendingAttempt(order.getId());
        if (pending != null) {
            Instant createdAt = pending.createdAt().toInstant(ZoneOffset.UTC);
            Instant expiresAt = createdAt.plusSeconds(ATTEMPT_LIFETIME_SECONDS);
            if (clock.instant().isBefore(expiresAt)) {
                // Reuse the attempt that is still valid instead of opening a second one.
                return gateway.url(pending.ref(), pending.amount(), clientIp, createdAt);
            }
            throw new BusinessRuleViolationException(
                "Phiên VNPay đã hết thời gian. Bấm Kiểm tra thanh toán để đối soát trước khi thanh toán lại."
            );
        }

        // One timestamp for both the stored attempt and the VNPay create date,
        // so a later querydr sends exactly the same vnp_TransactionDate.
        Instant now = clock.instant();
        String transactionRef = UUID.randomUUID().toString().replace("-", "");
        vnpayRepository.create(
            transactionRef,
            order.getId(),
            payment.getAmount(),
            LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        );
        return gateway.url(transactionRef, payment.getAmount(), clientIp, now);
    }

    @Override
    @Transactional
    public Map<String, String> handleIpn(Map<String, String> params) {
        if (!gateway.valid(params)) {
            return response(RSP_INVALID_SIGNATURE, "Invalid signature");
        }
        return applyVerifiedResult(params);
    }

    @Override
    public boolean isValidReturn(Map<String, String> params) {
        return gateway.valid(params);
    }

    @Override
    @Transactional
    public VnpayPendingAttempt findPendingAttempt(String orderCode) {
        long customerId = currentCustomerService.requireCurrentCustomer().customerId();
        SalesOrder order = vnpayRepository.lockOwned(orderCode, customerId);
        VnpayRepository.Pending pending = vnpayRepository.pendingAttempt(order.getId());
        if (pending == null) {
            return null;
        }
        return new VnpayPendingAttempt(pending.ref(), pending.amount(), pending.createdAt());
    }

    @Override
    @Transactional
    public Map<String, String> applyQueryResult(Map<String, String> params) {
        String transactionRef = params.getOrDefault("vnp_TxnRef", "");
        if (!gateway.validQuery(params, transactionRef)) {
            return response(RSP_INVALID_SIGNATURE, "Invalid signature");
        }
        boolean finalPaymentResult = VNPAY_SUCCESS.equals(params.get("vnp_ResponseCode"))
            && VNPAY_PAYMENT_TRANSACTION.equals(params.get("vnp_TransactionType"));
        if (!finalPaymentResult) {
            return response(RSP_OTHER, "Awaiting final transaction status");
        }
        return applyVerifiedResult(params);
    }

    /**
     * Applies an IPN or querydr result whose signature was already verified.
     * The order row is locked so duplicate callbacks are processed one by one.
     */
    private Map<String, String> applyVerifiedResult(Map<String, String> params) {
        String transactionRef = params.get("vnp_TxnRef");
        if (transactionRef == null) {
            return response(RSP_ORDER_NOT_FOUND, "Order not found");
        }
        Object[] attemptBeforeLock = vnpayRepository.attempt(transactionRef);
        if (attemptBeforeLock == null) {
            return response(RSP_ORDER_NOT_FOUND, "Order not found");
        }

        long orderId = ((Number) attemptBeforeLock[0]).longValue();
        SalesOrder order = vnpayRepository.lock(orderId);
        // Read the attempt again after the lock so we see the latest status.
        Object[] attempt = vnpayRepository.attempt(transactionRef);
        BigDecimal attemptAmount = (BigDecimal) attempt[1];
        String attemptStatus = (String) attempt[2];

        BigDecimal receivedAmount;
        try {
            // VNPay sends the amount multiplied by 100.
            receivedAmount = new BigDecimal(params.get("vnp_Amount")).movePointLeft(2);
        } catch (RuntimeException exception) {
            return response(RSP_INVALID_AMOUNT, "Invalid amount");
        }

        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        boolean amountMatches = payments.size() == 1
            && receivedAmount.compareTo(attemptAmount) == 0
            && receivedAmount.compareTo(order.getTotalAmount()) == 0;
        if (!amountMatches) {
            return response(RSP_INVALID_AMOUNT, "Invalid amount");
        }

        Payment payment = payments.get(0);
        if (!"PENDING".equals(attemptStatus) || payment.getStatus() == PaymentStatus.PAID) {
            return response(RSP_ALREADY_CONFIRMED, "Already confirmed");
        }
        if (order.getStatus() == OrderStatus.CANCELLED
            || payment.getMethod() != PaymentMethod.VNPAY) {
            return response(RSP_ORDER_NOT_FOUND, "Invalid order state");
        }

        boolean paid = VNPAY_SUCCESS.equals(params.get("vnp_ResponseCode"))
            && VNPAY_SUCCESS.equals(params.get("vnp_TransactionStatus"));
        String transactionNo = params.get("vnp_TransactionNo");
        if (paid && (transactionNo == null || !transactionNo.matches("[0-9]{1,50}"))) {
            return response(RSP_OTHER, "Invalid transaction");
        }
        // Pending, reversal and refund states are not safe evidence of a failed charge.
        boolean failed = !paid
            && VNPAY_TRANSACTION_FAILED.equals(params.get("vnp_TransactionStatus"));
        if (!paid && !failed) {
            return response(RSP_OTHER, "Awaiting final transaction status");
        }

        if (paid) {
            vnpayRepository.finish(transactionRef, "PAID", transactionNo);
            LocalDateTime now = LocalDateTime.now(clock);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(now);
            payment.setUpdatedAt(now);
            paymentRepository.save(payment);
        } else {
            vnpayRepository.finish(transactionRef, "FAILED", null);
        }
        return response(RSP_SUCCESS, "Confirm Success");
    }

    private Map<String, String> response(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
