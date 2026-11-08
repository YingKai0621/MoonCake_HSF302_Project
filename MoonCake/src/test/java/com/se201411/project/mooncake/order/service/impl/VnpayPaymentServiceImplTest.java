package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VnpayPaymentServiceImplTest {

    private final VnpayRepository vnpayRepository = mock(VnpayRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final VnpayGateway gateway = new VnpayGateway(new MockEnvironment()
        .withProperty("VNPAY_ENABLED", "true")
        .withProperty("VNPAY_TMN_CODE", "TESTCODE")
        .withProperty("VNPAY_HASH_SECRET", "test-only-secret"));
    private final CurrentCustomerService currentCustomerService = mock(CurrentCustomerService.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC);
    private final VnpayPaymentService service = new VnpayPaymentServiceImpl(
        vnpayRepository,
        paymentRepository,
        currentCustomerService,
        gateway,
        fixedClock
    );

    @Test
    void codOrderCannotSwitchToVnpay() {
        givenCustomerOrderSeven(PaymentMethod.COD);

        assertThatThrownBy(() -> service.startPayment("MC-7", "127.0.0.1"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("COD");

        verify(paymentRepository, never()).save(any());
        verify(vnpayRepository, never()).create(anyString(), anyLong(), any(), any());
    }

    @Test
    void newAttemptUsesTheSameTimestampForStorageAndGateway() {
        givenCustomerOrderSeven(PaymentMethod.VNPAY);

        String url = service.startPayment("MC-7", "127.0.0.1");

        verify(vnpayRepository).create(
            anyString(),
            eq(7L),
            eq(new BigDecimal("100000")),
            eq(LocalDateTime.of(2026, 11, 8, 3, 0))
        );
        // 03:00 UTC is 10:00 in Vietnam; expires 15 minutes later.
        assertThat(url).contains("vnp_CreateDate=20261108100000", "vnp_ExpireDate=20261108101500");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void validPendingAttemptIsReusedInsteadOfOpeningANewOne() {
        givenCustomerOrderSeven(PaymentMethod.VNPAY);
        when(vnpayRepository.pendingAttempt(7L)).thenReturn(new VnpayRepository.Pending(
            "old-ref",
            new BigDecimal("100000"),
            LocalDateTime.of(2026, 11, 8, 2, 50)
        ));

        String url = service.startPayment("MC-7", "127.0.0.1");

        assertThat(url).contains("vnp_TxnRef=old-ref");
        verify(vnpayRepository, never()).create(anyString(), anyLong(), any(), any());
    }

    @Test
    void noPendingAttemptAllowsCancellation() {
        when(vnpayRepository.pendingAttempt(7L)).thenReturn(null);

        service.requireNoOpenAttempt(7L);
    }

    @Test
    void attemptInsideItsWindowBlocksCancellation() {
        // Created 02:50 UTC, clock is 03:00 -> still inside the 15 minutes.
        when(vnpayRepository.pendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 50));

        assertThatThrownBy(() -> service.requireNoOpenAttempt(7L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("chưa kết thúc");
    }

    @Test
    void expiredButUnreconciledAttemptAsksForPaymentCheck() {
        when(vnpayRepository.pendingAttempt(7L)).thenReturn(pendingCreatedAt(2, 30));

        assertThatThrownBy(() -> service.requireNoOpenAttempt(7L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Kiểm tra thanh toán");
    }

    @Test
    void notFoundQueryClosesAttemptOnlyAfterExpiryMargin() {
        // Created 02:30 -> 30 minutes ago, past 15 minutes + 5 minutes margin.
        givenAttemptRefCreatedAt(2, 30);

        Map<String, String> result = service.applyQueryResult(signedQuery("91", "", ""));

        assertThat(result.get("RspCode")).isEqualTo("00");
        verify(vnpayRepository).finish("ref", "FAILED", null);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void notFoundQueryInsideMarginKeepsAttemptPending() {
        // Created 02:50 -> only 10 minutes ago, VNPay may still accept payment.
        givenAttemptRefCreatedAt(2, 50);

        Map<String, String> result = service.applyQueryResult(signedQuery("91", "", ""));

        assertThat(result.get("RspCode")).isEqualTo("99");
        verify(vnpayRepository, never()).finish(anyString(), anyString(), any());
    }

    @Test
    void unfinishedPaymentQueryClosesExpiredAttempt() {
        givenAttemptRefCreatedAt(2, 30);

        Map<String, String> result = service.applyQueryResult(signedQuery("00", "01", "01"));

        assertThat(result.get("RspCode")).isEqualTo("00");
        verify(vnpayRepository).finish("ref", "FAILED", null);
    }

    private void givenAttemptRefCreatedAt(int hour, int minute) {
        when(vnpayRepository.attempt("ref"))
            .thenReturn(new Object[] {7L, new BigDecimal("100000"), "PENDING"});
        when(vnpayRepository.pendingAttempt(7L)).thenReturn(pendingCreatedAt(hour, minute));
    }

    private VnpayRepository.Pending pendingCreatedAt(int hour, int minute) {
        return new VnpayRepository.Pending(
            "ref",
            new BigDecimal("100000"),
            LocalDateTime.of(2026, 11, 8, hour, minute)
        );
    }

    /** A signed querydr response for attempt "ref". */
    private Map<String, String> signedQuery(
        String responseCode,
        String transactionType,
        String transactionStatus
    ) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_ResponseId", "response");
        params.put("vnp_Command", "querydr");
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_Message", "msg");
        params.put("vnp_TmnCode", "TESTCODE");
        params.put("vnp_TxnRef", "ref");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TransactionType", transactionType);
        params.put("vnp_TransactionStatus", transactionStatus);
        String data = String.join("|",
            "response", "querydr", responseCode, "msg", "TESTCODE", "ref", "10000000",
            "", "", "", transactionType, transactionStatus, "", "", "");
        params.put("vnp_SecureHash", gateway.sign(data));
        return params;
    }

    /** Order 7 of customer 3: PLACED, 100.000đ, one UNPAID payment. */
    private void givenCustomerOrderSeven(PaymentMethod method) {
        when(currentCustomerService.requireCurrentCustomer())
            .thenReturn(ShopperIdentity.forCustomer(3L));
        SalesOrder order = mock(SalesOrder.class);
        when(order.getId()).thenReturn(7L);
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);
        when(order.getTotalAmount()).thenReturn(new BigDecimal("100000"));
        when(vnpayRepository.lockOwned("MC-7", 3L)).thenReturn(order);

        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setAmount(new BigDecimal("100000"));
        when(paymentRepository.findByOrderId(7L)).thenReturn(List.of(payment));
    }

    @Test
    void publicUrlBuildsReturnUrl() {
        VnpayGateway publicGateway = new VnpayGateway(new MockEnvironment()
            .withProperty("APP_PUBLIC_URL", "https://demo.example.test/"));

        assertThat(publicGateway.returnUrl())
            .isEqualTo("https://demo.example.test/payments/vnpay/return");
    }

    @Test
    void enabledWithoutCredentialsFailsAtStartup() {
        VnpayGateway incompleteGateway = new VnpayGateway(new MockEnvironment()
            .withProperty("VNPAY_ENABLED", "true"));

        assertThatThrownBy(incompleteGateway::validateStartup)
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void tamperedSignatureCannotWrite() {
        Map<String, String> params = signedCallback();
        params.put("vnp_Amount", "1");

        assertThat(service.handleIpn(params).get("RspCode")).isEqualTo("97");
        verifyNoInteractions(vnpayRepository, paymentRepository);
    }

    @Test
    void wrongAmountCannotMarkPaid() {
        givenPendingAttemptForOrderOne();
        Map<String, String> params = signedCallback();
        params.put("vnp_Amount", "999");
        resign(params);

        assertThat(service.handleIpn(params).get("RspCode")).isEqualTo("04");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void validIpnMarksPaid() {
        givenPendingAttemptForOrderOne();

        assertThat(service.handleIpn(signedCallback()).get("RspCode")).isEqualTo("00");
        Payment payment = paymentRepository.findByOrderId(1L).get(0);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(vnpayRepository).finish("ref", "PAID", "12345");
    }

    @Test
    void replayDoesNotPayTwice() {
        givenPendingAttemptForOrderOne();

        service.handleIpn(signedCallback());
        assertThat(service.handleIpn(signedCallback()).get("RspCode")).isEqualTo("02");
        verify(paymentRepository, times(1)).save(any());
    }

    @Test
    void cancelledOrderCannotBePaid() {
        SalesOrder order = givenPendingAttemptForOrderOne();
        when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);

        assertThat(service.handleIpn(signedCallback()).get("RspCode")).isEqualTo("01");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void failureDoesNotPay() {
        givenPendingAttemptForOrderOne();
        Map<String, String> params = signedCallback();
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionStatus", "02");
        resign(params);

        assertThat(service.handleIpn(params).get("RspCode")).isEqualTo("00");
        verify(vnpayRepository).finish("ref", "FAILED", null);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void buildsSandboxUrlWithAmountInHundredths() {
        String url = gateway.url(
            "ref",
            new BigDecimal("100000"),
            "127.0.0.1",
            Instant.parse("2026-11-02T00:00:00Z")
        );

        assertThat(url).contains(
            "vnp_Amount=10000000",
            "vnp_Version=2.1.0",
            "vnp_CreateDate=20261102070000",
            "vnp_ExpireDate=20261102071500",
            "vnp_SecureHash="
        );
    }

    @Test
    void pendingOrReversalCannotReleasePaymentForRetry() {
        givenPendingAttemptForOrderOne();

        for (String state : List.of("01", "04", "05", "06", "07", "09")) {
            Map<String, String> params = signedCallback();
            params.put("vnp_ResponseCode", "99");
            params.put("vnp_TransactionStatus", state);
            resign(params);

            assertThat(service.handleIpn(params).get("RspCode")).isEqualTo("99");
        }
        verify(vnpayRepository, never()).finish(anyString(), anyString(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void disabledGatewayRejectsPublicCallbackWithoutException() {
        VnpayGateway disabledGateway = new VnpayGateway(new MockEnvironment());

        assertThat(disabledGateway.valid(signedCallback())).isFalse();
    }

    @Test
    void querySignatureProtectsAmountReferenceAndTransactionType() {
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

        givenPendingAttemptForOrderOne();
        assertThat(service.applyQueryResult(params).get("RspCode")).isEqualTo("00");
        verify(vnpayRepository).finish("ref", "PAID", "12345");

        params.put("vnp_Amount", "1");
        assertThat(gateway.validQuery(params, "ref")).isFalse();
    }

    /** A successful, correctly signed IPN for attempt "ref" of 100.000đ. */
    private Map<String, String> signedCallback() {
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

    /** Order 1 (PLACED, 100.000đ) with one VNPay payment and a PENDING attempt "ref". */
    private SalesOrder givenPendingAttemptForOrderOne() {
        SalesOrder order = mock(SalesOrder.class);
        when(order.getId()).thenReturn(1L);
        when(order.getTotalAmount()).thenReturn(new BigDecimal("100000"));
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);
        when(vnpayRepository.attempt("ref"))
            .thenReturn(new Object[] {1L, new BigDecimal("100000"), "PENDING"});
        when(vnpayRepository.lock(1L)).thenReturn(order);

        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setAmount(new BigDecimal("100000"));
        when(paymentRepository.findByOrderId(1L)).thenReturn(List.of(payment));
        return order;
    }
}
