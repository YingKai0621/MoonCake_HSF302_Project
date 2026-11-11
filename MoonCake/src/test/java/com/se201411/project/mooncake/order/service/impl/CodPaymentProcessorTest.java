package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodPaymentProcessorTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final CodPaymentProcessor processor = new CodPaymentProcessor(paymentRepository);

    @Test
    void codAcceptsOrdersUpToFiveMillion() {
        assertThat(processor.acceptsTotal(BigDecimal.valueOf(5_000_000))).isTrue();
        assertThat(processor.acceptsTotal(BigDecimal.valueOf(5_001_000))).isFalse();
    }

    @Test
    void largeCodOrderIsRefused() {
        assertThatThrownBy(() -> processor.checkCanPlace(7L, BigDecimal.valueOf(49_343_000)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("VNPay");
    }

    @Test
    void thirdOpenCodOrderIsRefused() {
        when(paymentRepository.countCustomerOrders(7L, OrderStatus.PLACED, PaymentMethod.COD, PaymentStatus.UNPAID))
            .thenReturn(2L);

        assertThatThrownBy(() -> processor.checkCanPlace(7L, BigDecimal.valueOf(300_000)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("2 đơn COD chờ xử lý");
    }

    @Test
    void smallCodOrderWithOneOpenOrderIsAllowed() {
        when(paymentRepository.countCustomerOrders(7L, OrderStatus.PLACED, PaymentMethod.COD, PaymentStatus.UNPAID))
            .thenReturn(1L);

        assertThatCode(() -> processor.checkCanPlace(7L, BigDecimal.valueOf(300_000)))
            .doesNotThrowAnyException();
    }
}
