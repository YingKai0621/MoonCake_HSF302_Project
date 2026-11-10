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
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VnpayPaymentProcessorTest {
    private static final BigDecimal AMOUNT = new BigDecimal("100000");

    private final VnpayRepository vnpayRepository = mock(VnpayRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final VnpayGatewayImpl gateway = spy(new VnpayGatewayImpl(new MockEnvironment()
        .withProperty("VNPAY_ENABLED", "true")
        .withProperty("VNPAY_TMN_CODE", "TESTCODE")
        .withProperty("VNPAY_HASH_SECRET", "test-only-secret")));
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC);
    private final VnpayPaymentProcessor processor = new VnpayPaymentProcessor(
        vnpayRepository,
        paymentRepository,
        gateway,
        clock
    );

    @Test
    void tamperedSignatureCannotWrite() {
        Map<String, String> params = signedIpn();
        params.put("vnp_Amount", "1");

        assertThat(processor.handleNotification(params).get("RspCode")).isEqualTo("97");
        verifyNoInteractions(vnpayRepository, paymentRepository);
    }

    @Test
    void wrongAmountCannotMarkPaid() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);
        Map<String, String> params = signedIpn();
        params.put("vnp_Amount", "999");
        resign(params);

        assertThat(processor.handleNotification(params).get("RspCode")).isEqualTo("04");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void validIpnMarksPaid() {
        Payment payment = givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);

        assertThat(processor.handleNotification(signedIpn()).get("RspCode")).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(vnpayRepository).markPaid("ref", "12345");
    }

    @Test
    void replayDoesNotPayTwice() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);

        processor.handleNotification(signedIpn());
        assertThat(processor.handleNotification(signedIpn()).get("RspCode")).isEqualTo("02");
        verify(paymentRepository, times(1)).save(any());
    }

    @Test
    void latePaymentAfterCancellationIsRecordedForRefund() {
        Payment payment = givenOrderOne(OrderStatus.CANCELLED, "FAILED", PaymentMethod.VNPAY);

        assertThat(processor.handleNotification(signedIpn()).get("RspCode")).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(vnpayRepository).markPaid("ref", "12345");
    }

    @Test
    void codPaymentCannotBePaidThroughVnpay() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.COD);

        assertThat(processor.handleNotification(signedIpn()).get("RspCode")).isEqualTo("01");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void failureDoesNotPay() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);
        Map<String, String> params = signedIpn();
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionStatus", "02");
        resign(params);

        assertThat(processor.handleNotification(params).get("RspCode")).isEqualTo("00");
        verify(vnpayRepository).markFailed("ref");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pendingOrReversalStatesChangeNothing() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);

        for (String state : List.of("01", "04", "05", "06", "07", "09")) {
            Map<String, String> params = signedIpn();
            params.put("vnp_ResponseCode", "99");
            params.put("vnp_TransactionStatus", state);
            resign(params);

            assertThat(processor.handleNotification(params).get("RspCode")).isEqualTo("99");
        }
        verify(vnpayRepository, never()).markFailed(anyString());
        verify(vnpayRepository, never()).markPaid(anyString(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void returnWithBadSignatureIsNotTrusted() {
        Map<String, String> params = signedIpn();
        params.put("vnp_Amount", "1");

        assertThat(processor.handleReturn(params).verified()).isFalse();
        verifyNoInteractions(vnpayRepository, paymentRepository);
    }

    @Test
    void returnAfterPaymentAsksVnpayAndShowsPaid() {
        Payment payment = givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);
        Map<String, String> paidResult = queryResult("00", "01", "00");
        paidResult.put("vnp_TransactionNo", "12345");
        givenVnpayAnswers(paidResult);

        PaymentReturnResult result = processor.handleReturn(signedIpn());

        assertThat(result.verified()).isTrue();
        assertThat(result.orderCode()).isEqualTo("MC-1");
        assertThat(result.paid()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void returnAfterCustomerCancelledOnVnpayDoesNotAskVnpay() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);
        Map<String, String> params = signedIpn();
        params.put("vnp_ResponseCode", "24");
        resign(params);

        PaymentReturnResult result = processor.handleReturn(params);

        assertThat(result.completedOnGateway()).isFalse();
        assertThat(result.paid()).isFalse();
        verify(gateway, never()).query(anyString(), any(), anyString(), any());
    }

    @Test
    void returnWhenVnpayIsUnreachableStillLeadsToTheOrder() {
        givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);
        doThrow(new BusinessRuleViolationException("offline"))
            .when(gateway).query(anyString(), any(), anyString(), any());

        PaymentReturnResult result = processor.handleReturn(signedIpn());

        assertThat(result.verified()).isTrue();
        assertThat(result.completedOnGateway()).isTrue();
        assertThat(result.paid()).isFalse();
    }

    @Test
    void codOrderCannotSwitchToVnpay() {
        SalesOrder order = givenOrderSeven(PaymentMethod.COD);

        assertThatThrownBy(() -> processor.startPayment(order, "127.0.0.1"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("COD");
        verify(vnpayRepository, never()).createAttempt(anyString(), anyLong(), any(), any());
    }

    @Test
    void newAttemptUsesOneTimestampForStorageAndGateway() {
        SalesOrder order = givenOrderSeven(PaymentMethod.VNPAY);

        Optional<String> url = processor.startPayment(order, "127.0.0.1");

        verify(vnpayRepository).createAttempt(
            anyString(),
            eq(7L),
            eq(AMOUNT),
            eq(LocalDateTime.of(2026, 11, 8, 3, 0))
        );
        assertThat(url.get()).contains("vnp_CreateDate=20261108100000", "vnp_ExpireDate=20261108101500");
    }

    @Test
    void attemptInsideItsWindowIsReused() {
        SalesOrder order = givenOrderSeven(PaymentMethod.VNPAY);
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 50));

        Optional<String> url = processor.startPayment(order, "127.0.0.1");

        assertThat(url.get()).contains("vnp_TxnRef=ref");
        verify(vnpayRepository, never()).createAttempt(anyString(), anyLong(), any(), any());
    }

    @Test
    void expiredUnpaidAttemptIsClosedAndANewOneOpened() {
        SalesOrder order = givenOrderSeven(PaymentMethod.VNPAY);
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 30));
        givenVnpayAnswers(queryResult("91", "", ""));

        processor.startPayment(order, "127.0.0.1");

        verify(vnpayRepository).markFailed("ref");
        verify(vnpayRepository).createAttempt(anyString(), eq(7L), eq(AMOUNT), any());
    }

    @Test
    void cancelWithoutOpenAttemptDoesNotAskVnpay() {
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(null);

        processor.checkCanCancel(7L, payment(PaymentStatus.UNPAID));

        verify(gateway, never()).query(anyString(), any(), anyString(), any());
    }

    @Test
    void cancelClosesAttemptWhenVnpayHasNoPayment() {
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 58));
        givenVnpayAnswers(queryResult("00", "01", "01"));

        processor.checkCanCancel(7L, payment(PaymentStatus.UNPAID));

        verify(vnpayRepository).markFailed("ref");
    }

    @Test
    void cancelIsRefusedWhenVnpaySaysPaid() {
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 58));
        givenVnpayAnswers(queryResult("00", "01", "00"));

        assertThatThrownBy(() -> processor.checkCanCancel(7L, payment(PaymentStatus.UNPAID)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đã được thanh toán");
        verify(vnpayRepository, never()).markFailed(anyString());
    }

    @Test
    void cancelGoesAheadWhenVnpayGivesNoClearAnswer() {
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 58));
        givenVnpayAnswers(queryResult("99", "", ""));

        processor.checkCanCancel(7L, payment(PaymentStatus.UNPAID));

        verify(vnpayRepository).markFailed("ref");
    }

    @Test
    void cancelGoesAheadWhenVnpayIsUnreachable() {
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 58));
        doThrow(new BusinessRuleViolationException("offline"))
            .when(gateway).query(anyString(), any(), anyString(), any());

        processor.checkCanCancel(7L, payment(PaymentStatus.UNPAID));

        verify(vnpayRepository).markFailed("ref");
    }

    @Test
    void paidOrderSkipsVnpayCheckBecauseAdminRefundsManually() {
        processor.checkCanCancel(7L, payment(PaymentStatus.PAID));

        verifyNoInteractions(vnpayRepository);
    }

    @Test
    void unpaidOrderCannotBeFulfilledOrCompleted() {
        Payment unpaid = payment(PaymentStatus.UNPAID);

        assertThatThrownBy(() -> processor.checkCanFulfil(unpaid))
            .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> processor.completeDelivery(7L, unpaid, LocalDateTime.now(clock)))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void adminRefreshRecordsPaymentReportedByVnpay() {
        Payment payment = givenOrderOne(OrderStatus.PLACED, "PENDING", PaymentMethod.VNPAY);
        when(vnpayRepository.findPendingAttempt(1L)).thenReturn(pendingCreatedAt(2, 58));
        Map<String, String> paidResult = queryResult("00", "01", "00");
        paidResult.put("vnp_TransactionNo", "12345");
        givenVnpayAnswers(paidResult);

        processor.refreshPaymentStatus(1L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(vnpayRepository).markPaid("ref", "12345");
    }

    @Test
    void sandboxUrlSendsAmountInHundredths() {
        String url = gateway.url("ref", AMOUNT, "127.0.0.1", Instant.parse("2026-11-02T00:00:00Z"));

        assertThat(url).contains(
            "vnp_Amount=10000000",
            "vnp_Version=2.1.0",
            "vnp_CreateDate=20261102070000",
            "vnp_ExpireDate=20261102071500",
            "vnp_SecureHash="
        );
    }

    @Test
    void publicUrlBuildsReturnUrl() {
        VnpayGatewayImpl publicGateway = new VnpayGatewayImpl(new MockEnvironment()
            .withProperty("APP_PUBLIC_URL", "https://demo.example.test/"));

        assertThat(publicGateway.returnUrl()).isEqualTo("https://demo.example.test/payments/vnpay/return");
    }

    @Test
    void enabledWithoutCredentialsFailsAtStartup() {
        VnpayGatewayImpl incompleteGateway = new VnpayGatewayImpl(new MockEnvironment()
            .withProperty("VNPAY_ENABLED", "true"));

        assertThatThrownBy(incompleteGateway::validateStartup)
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void disabledGatewayRejectsCallbackWithoutException() {
        VnpayGatewayImpl disabledGateway = new VnpayGatewayImpl(new MockEnvironment());

        assertThat(disabledGateway.valid(signedIpn())).isFalse();
    }

    @Test
    void querySignatureProtectsAmountAndReference() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_ResponseId", "response");
        params.put("vnp_Command", "querydr");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_Message", "Success");
        params.put("vnp_TmnCode", "TESTCODE");
        params.put("vnp_TxnRef", "ref");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_TransactionNo", "12345");
        params.put("vnp_TransactionType", "01");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_OrderInfo", "test");
        params.put("vnp_SecureHash", gateway.sign(
            "response|querydr|00|Success|TESTCODE|ref|10000000|NCB||12345|01|00|test||"
        ));

        assertThat(gateway.validQuery(params, "ref")).isTrue();
        assertThat(gateway.validQuery(params, "other")).isFalse();
        params.put("vnp_Amount", "1");
        assertThat(gateway.validQuery(params, "ref")).isFalse();
    }

    private Map<String, String> signedIpn() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "TESTCODE");
        params.put("vnp_TxnRef", "ref");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", "12345");
        params.put("vnp_SecureHash", gateway.signature(params));
        return params;
    }

    private void resign(Map<String, String> params) {
        params.put("vnp_SecureHash", gateway.signature(params));
    }

    private Map<String, String> queryResult(
        String responseCode,
        String transactionType,
        String transactionStatus
    ) {
        Map<String, String> result = new HashMap<>();
        result.put("vnp_ResponseCode", responseCode);
        result.put("vnp_TxnRef", "ref");
        result.put("vnp_Amount", "10000000");
        result.put("vnp_TransactionType", transactionType);
        result.put("vnp_TransactionStatus", transactionStatus);
        return result;
    }

    private void givenVnpayAnswers(Map<String, String> result) {
        doReturn(result).when(gateway).query(anyString(), any(), anyString(), any());
    }

    private Payment givenOrderOne(OrderStatus orderStatus, String attemptStatus, PaymentMethod method) {
        SalesOrder order = mock(SalesOrder.class);
        when(order.getId()).thenReturn(1L);
        when(order.getTotalAmount()).thenReturn(AMOUNT);
        when(order.getStatus()).thenReturn(orderStatus);
        when(order.getOrderCode()).thenReturn("MC-1");
        when(vnpayRepository.findAttempt("ref")).thenReturn(new VnpayAttemptView(
            "ref",
            1L,
            AMOUNT,
            attemptStatus,
            LocalDateTime.of(2026, 11, 8, 2, 58)
        ));
        when(vnpayRepository.lockOrder(1L)).thenReturn(order);

        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setAmount(AMOUNT);
        when(paymentRepository.findByOrderId(1L)).thenReturn(List.of(payment));
        return payment;
    }

    @Test
    void unpaidOrderIsCancelledThirtyMinutesAfterItWasPlaced() {
        SalesOrder order = placedAt(2, 0);
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(null);

        Optional<LocalDateTime> deadline = processor.autoCancelAt(order, payment(PaymentStatus.UNPAID));

        assertThat(deadline).contains(LocalDateTime.of(2026, 11, 8, 2, 30));
    }

    @Test
    void openAttemptPushesTheDeadlineToItsExpiry() {
        SalesOrder order = placedAt(2, 0);
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 25));

        Optional<LocalDateTime> deadline = processor.autoCancelAt(order, payment(PaymentStatus.UNPAID));

        assertThat(deadline).contains(LocalDateTime.of(2026, 11, 8, 2, 40));
    }

    @Test
    void shorterLifetimesCanBeConfiguredForTesting() {
        VnpayGatewayImpl quickGateway = new VnpayGatewayImpl(new MockEnvironment()
            .withProperty("UNPAID_ORDER_CANCEL_MINUTES", "2")
            .withProperty("VNPAY_ATTEMPT_SECONDS", "60"));
        VnpayPaymentProcessor quickProcessor = new VnpayPaymentProcessor(
            vnpayRepository,
            paymentRepository,
            quickGateway,
            clock
        );
        SalesOrder order = placedAt(2, 0);
        when(vnpayRepository.findPendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 1, 30));

        Optional<LocalDateTime> deadline = quickProcessor.autoCancelAt(order, payment(PaymentStatus.UNPAID));

        assertThat(deadline).contains(LocalDateTime.of(2026, 11, 8, 2, 2, 30));
    }

    @Test
    void paidOrderHasNoDeadline() {
        SalesOrder order = mock(SalesOrder.class);
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);

        Optional<LocalDateTime> deadline = processor.autoCancelAt(order, payment(PaymentStatus.PAID));

        assertThat(deadline).isEmpty();
    }

    private SalesOrder placedAt(int hour, int minute) {
        SalesOrder order = mock(SalesOrder.class);
        when(order.getId()).thenReturn(7L);
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);
        when(order.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 11, 8, hour, minute));
        return order;
    }

    private SalesOrder givenOrderSeven(PaymentMethod method) {
        SalesOrder order = mock(SalesOrder.class);
        when(order.getId()).thenReturn(7L);
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);
        when(order.getTotalAmount()).thenReturn(AMOUNT);

        Payment payment = payment(PaymentStatus.UNPAID);
        payment.setMethod(method);
        when(paymentRepository.findByOrderId(7L)).thenReturn(List.of(payment));
        return order;
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(status);
        payment.setAmount(AMOUNT);
        return payment;
    }

    private VnpayAttemptView pendingCreatedAt(int hour, int minute, int second) {
        return new VnpayAttemptView(
            "ref",
            7L,
            AMOUNT,
            "PENDING",
            LocalDateTime.of(2026, 11, 8, hour, minute, second)
        );
    }

    private VnpayAttemptView pendingCreatedAt(int hour, int minute) {
        return new VnpayAttemptView(
            "ref",
            7L,
            AMOUNT,
            "PENDING",
            LocalDateTime.of(2026, 11, 8, hour, minute)
        );
    }
}
