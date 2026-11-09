package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.PaymentReturnResult;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.dto.VnpayAttemptView;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VnpayPaymentProcessor implements PaymentProcessor {
    private static final String SERVER_IP = "127.0.0.1";

    private static final String RSP_SUCCESS = "00";
    private static final String RSP_ORDER_NOT_FOUND = "01";
    private static final String RSP_ALREADY_CONFIRMED = "02";
    private static final String RSP_INVALID_AMOUNT = "04";
    private static final String RSP_INVALID_SIGNATURE = "97";
    private static final String RSP_OTHER = "99";

    private static final String VNPAY_SUCCESS = "00";
    private static final String VNPAY_NOT_COMPLETED = "01";
    private static final String VNPAY_FAILED = "02";
    private static final String VNPAY_PAYMENT_TRANSACTION = "01";
    private static final String QUERY_NOT_FOUND = "91";

    private enum QueryOutcome {
        PAID,
        NOT_PAID,
        UNKNOWN
    }

    private final VnpayRepository vnpayRepository;
    private final PaymentRepository paymentRepository;
    private final VnpayGateway gateway;
    private final Clock clock;

    public VnpayPaymentProcessor(
        VnpayRepository vnpayRepository,
        PaymentRepository paymentRepository,
        VnpayGateway gateway,
        Clock clock
    ) {
        this.vnpayRepository = vnpayRepository;
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public String displayName() {
        return "VNPay";
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return gateway.enabled();
    }

    @Override
    public Optional<String> startPayment(SalesOrder order, String clientIp) {
        gateway.requireConfigured();
        Payment payment = requireSinglePayment(order.getId());

        boolean canPayOnline = order.getStatus() == OrderStatus.PLACED
            && payment.getStatus() == PaymentStatus.UNPAID
            && order.getTotalAmount().signum() > 0;
        if (!canPayOnline) {
            throw new BusinessRuleViolationException(
                "Đơn hàng này không còn chờ thanh toán. Vui lòng tải lại trang."
            );
        }
        if (payment.getMethod() != PaymentMethod.VNPAY) {
            throw new BusinessRuleViolationException(
                "Đơn này thanh toán khi nhận hàng (COD) nên không thể chuyển sang VNPay. "
                    + "Muốn thanh toán online, vui lòng hủy đơn và đặt lại."
            );
        }

        VnpayAttemptView pending = vnpayRepository.findPendingAttempt(order.getId());
        if (pending != null) {
            if (!isExpired(pending)) {
                Instant createdAt = pending.createdAt().toInstant(ZoneOffset.UTC);
                String url = gateway.url(pending.transactionRef(), pending.amount(), clientIp, createdAt);
                return Optional.of(url);
            }
            closeUnpaidAttempt(pending);
        }

        Instant now = clock.instant();
        String transactionRef = UUID.randomUUID().toString().replace("-", "");
        vnpayRepository.createAttempt(
            transactionRef,
            order.getId(),
            payment.getAmount(),
            LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        );
        return Optional.of(gateway.url(transactionRef, payment.getAmount(), clientIp, now));
    }

    @Override
    public void checkCanCancel(long orderId, Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        VnpayAttemptView pending = vnpayRepository.findPendingAttempt(orderId);
        if (pending == null) {
            return;
        }
        closeUnpaidAttempt(pending);
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
        checkCanFulfil(payment);
    }

    @Override
    public void refreshPaymentStatus(long orderId) {
        VnpayAttemptView pending = vnpayRepository.findPendingAttempt(orderId);
        if (pending == null) {
            return;
        }
        refreshFromVnpay(pending);
    }

    @Override
    public Optional<LocalDateTime> autoCancelAt(SalesOrder order, Payment payment) {
        if (order.getStatus() != OrderStatus.PLACED || payment.getStatus() != PaymentStatus.UNPAID) {
            return Optional.empty();
        }
        LocalDateTime deadline = order.getCreatedAt().plusMinutes(gateway.unpaidOrderLifetimeMinutes());
        VnpayAttemptView pending = vnpayRepository.findPendingAttempt(order.getId());
        if (pending != null) {
            LocalDateTime attemptEnd = pending.createdAt().plusSeconds(gateway.attemptLifetimeSeconds());
            if (attemptEnd.isAfter(deadline)) {
                deadline = attemptEnd;
            }
        }
        return Optional.of(deadline);
    }

    private void refreshFromVnpay(VnpayAttemptView pending) {
        Map<String, String> result = queryVnpay(pending);
        QueryOutcome outcome = outcomeOf(result);
        if (outcome == QueryOutcome.PAID) {
            applyVerifiedResult(result);
            return;
        }
        if (outcome == QueryOutcome.NOT_PAID && isExpired(pending)) {
            vnpayRepository.markFailed(pending.transactionRef());
        }
    }

    @Override
    @Transactional
    public Map<String, String> handleNotification(Map<String, String> params) {
        if (!gateway.valid(params)) {
            return response(RSP_INVALID_SIGNATURE, "Invalid signature");
        }
        return applyVerifiedResult(params);
    }

    @Override
    @Transactional
    public PaymentReturnResult handleReturn(Map<String, String> params) {
        if (!gateway.valid(params)) {
            return PaymentReturnResult.unverified();
        }
        VnpayAttemptView attempt = vnpayRepository.findAttempt(params.get("vnp_TxnRef"));
        if (attempt == null) {
            return PaymentReturnResult.unverified();
        }

        String orderCode = vnpayRepository.lockOrder(attempt.orderId()).getOrderCode();
        boolean completedOnGateway = VNPAY_SUCCESS.equals(params.get("vnp_ResponseCode"));
        if (completedOnGateway && "PENDING".equals(attempt.status())) {
            try {
                refreshFromVnpay(attempt);
            } catch (BusinessRuleViolationException exception) {
                return new PaymentReturnResult(true, orderCode, true, false);
            }
        }

        Payment payment = requireSinglePayment(attempt.orderId());
        boolean paid = payment.getStatus() == PaymentStatus.PAID;
        return new PaymentReturnResult(true, orderCode, completedOnGateway, paid);
    }

    private void closeUnpaidAttempt(VnpayAttemptView pending) {
        if (isPaidAtVnpay(pending)) {
            throw new BusinessRuleViolationException(
                "Đơn đã được thanh toán qua VNPay. Vui lòng tải lại trang hoặc liên hệ cửa hàng để được hỗ trợ."
            );
        }
        vnpayRepository.markFailed(pending.transactionRef());
    }

    private boolean isPaidAtVnpay(VnpayAttemptView pending) {
        try {
            Map<String, String> result = queryVnpay(pending);
            return outcomeOf(result) == QueryOutcome.PAID;
        } catch (BusinessRuleViolationException exception) {
            return false;
        }
    }

    private Map<String, String> queryVnpay(VnpayAttemptView pending) {
        Map<String, String> result = gateway.query(
            pending.transactionRef(),
            pending.createdAt(),
            SERVER_IP,
            clock.instant()
        );
        return result;
    }

    private QueryOutcome outcomeOf(Map<String, String> result) {
        String responseCode = result.get("vnp_ResponseCode");
        if (QUERY_NOT_FOUND.equals(responseCode)) {
            return QueryOutcome.NOT_PAID;
        }
        boolean paymentQuery = VNPAY_SUCCESS.equals(responseCode)
            && VNPAY_PAYMENT_TRANSACTION.equals(result.get("vnp_TransactionType"));
        if (!paymentQuery) {
            return QueryOutcome.UNKNOWN;
        }
        String transactionStatus = result.get("vnp_TransactionStatus");
        if (VNPAY_SUCCESS.equals(transactionStatus)) {
            return QueryOutcome.PAID;
        }
        if (VNPAY_NOT_COMPLETED.equals(transactionStatus) || VNPAY_FAILED.equals(transactionStatus)) {
            return QueryOutcome.NOT_PAID;
        }
        return QueryOutcome.UNKNOWN;
    }

    private boolean isExpired(VnpayAttemptView attempt) {
        LocalDateTime expiresAt = attempt.createdAt().plusSeconds(gateway.attemptLifetimeSeconds());
        return !LocalDateTime.now(clock).isBefore(expiresAt);
    }

    private Map<String, String> applyVerifiedResult(Map<String, String> params) {
        String transactionRef = params.get("vnp_TxnRef");
        if (transactionRef == null) {
            return response(RSP_ORDER_NOT_FOUND, "Order not found");
        }
        VnpayAttemptView attemptBeforeLock = vnpayRepository.findAttempt(transactionRef);
        if (attemptBeforeLock == null) {
            return response(RSP_ORDER_NOT_FOUND, "Order not found");
        }

        SalesOrder order = vnpayRepository.lockOrder(attemptBeforeLock.orderId());
        VnpayAttemptView attempt = vnpayRepository.findAttempt(transactionRef);

        BigDecimal receivedAmount;
        try {
            receivedAmount = new BigDecimal(params.get("vnp_Amount")).movePointLeft(2);
        } catch (RuntimeException exception) {
            return response(RSP_INVALID_AMOUNT, "Invalid amount");
        }

        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        boolean amountMatches = payments.size() == 1
            && receivedAmount.compareTo(attempt.amount()) == 0
            && receivedAmount.compareTo(order.getTotalAmount()) == 0;
        if (!amountMatches) {
            return response(RSP_INVALID_AMOUNT, "Invalid amount");
        }

        Payment payment = payments.get(0);
        if (payment.getMethod() != PaymentMethod.VNPAY) {
            return response(RSP_ORDER_NOT_FOUND, "Invalid order state");
        }
        if ("PAID".equals(attempt.status()) || payment.getStatus() == PaymentStatus.PAID) {
            return response(RSP_ALREADY_CONFIRMED, "Already confirmed");
        }

        boolean paid = VNPAY_SUCCESS.equals(params.get("vnp_ResponseCode"))
            && VNPAY_SUCCESS.equals(params.get("vnp_TransactionStatus"));
        if (paid) {
            String transactionNo = params.get("vnp_TransactionNo");
            if (transactionNo == null || !transactionNo.matches("[0-9]{1,50}")) {
                return response(RSP_OTHER, "Invalid transaction");
            }
            vnpayRepository.markPaid(transactionRef, transactionNo);
            LocalDateTime now = LocalDateTime.now(clock);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(now);
            payment.setUpdatedAt(now);
            paymentRepository.save(payment);
            return response(RSP_SUCCESS, "Confirm Success");
        }

        if (!"PENDING".equals(attempt.status())) {
            return response(RSP_ALREADY_CONFIRMED, "Already confirmed");
        }
        if (VNPAY_FAILED.equals(params.get("vnp_TransactionStatus"))) {
            vnpayRepository.markFailed(transactionRef);
            return response(RSP_SUCCESS, "Confirm Success");
        }
        return response(RSP_OTHER, "Awaiting final transaction status");
    }

    private Payment requireSinglePayment(long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.size() != 1) {
            throw new BusinessRuleViolationException(
                "Đơn phải có đúng một bản ghi thanh toán."
            );
        }
        return payments.get(0);
    }

    private Map<String, String> response(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
