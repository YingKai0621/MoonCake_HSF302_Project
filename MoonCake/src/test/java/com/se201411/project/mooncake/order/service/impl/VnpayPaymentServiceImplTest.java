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
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private final VnpayPaymentService service = new VnpayPaymentServiceImpl(
        vnpayRepository,
        paymentRepository,
        mock(CurrentCustomerService.class),
        gateway,
        Clock.systemUTC()
    );

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
