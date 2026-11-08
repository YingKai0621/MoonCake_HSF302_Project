package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class VnpayPaymentProcessorTest {

    private final VnpayPaymentService vnpayPaymentService = mock(VnpayPaymentService.class);
    private final VnpayPaymentProcessor processor = new VnpayPaymentProcessor(vnpayPaymentService);

    @Test
    void unpaidOrderMustHaveNoOpenAttemptBeforeCancelling() {
        processor.checkCanCancel(5L, payment(PaymentStatus.UNPAID));

        verify(vnpayPaymentService).requireNoOpenAttempt(5L);
    }

    @Test
    void paidOrderSkipsAttemptCheckBecauseAdminRefundsManually() {
        processor.checkCanCancel(5L, payment(PaymentStatus.PAID));

        verify(vnpayPaymentService, never()).requireNoOpenAttempt(anyLong());
    }

    @Test
    void unpaidOrderCannotBeFulfilledOrCompleted() {
        Payment unpaid = payment(PaymentStatus.UNPAID);

        assertThatThrownBy(() -> processor.checkCanFulfil(unpaid))
            .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> processor.completeDelivery(5L, unpaid, LocalDateTime.now()))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(status);
        return payment;
    }
}
