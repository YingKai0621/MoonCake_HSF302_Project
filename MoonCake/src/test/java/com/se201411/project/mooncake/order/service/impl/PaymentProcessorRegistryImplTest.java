package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PaymentProcessorRegistryImplTest {
    private final PaymentProcessor cod = new CodPaymentProcessor(mock(PaymentRepository.class));
    private final PaymentProcessor vnpay = new VnpayPaymentProcessor(
        mock(VnpayRepository.class),
        mock(PaymentRepository.class),
        mock(VnpayGateway.class),
        Clock.systemUTC()
    );

    @Test
    void returnsTheProcessorForEachMethod() {
        PaymentProcessorRegistry registry = new PaymentProcessorRegistryImpl(List.of(cod, vnpay));

        assertThat(registry.forMethod(PaymentMethod.COD)).isSameAs(cod);
        assertThat(registry.forMethod(PaymentMethod.VNPAY)).isSameAs(vnpay);
    }

    @Test
    void everyPaymentMethodHasAProcessor() {
        PaymentProcessorRegistry registry = new PaymentProcessorRegistryImpl(List.of(cod, vnpay));

        for (PaymentMethod method : PaymentMethod.values()) {
            assertThat(registry.forMethod(method).method()).isEqualTo(method);
        }
    }

    @Test
    void missingProcessorFailsClearly() {
        PaymentProcessorRegistry registry = new PaymentProcessorRegistryImpl(List.of(cod));

        assertThatThrownBy(() -> registry.forMethod(PaymentMethod.VNPAY))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void twoProcessorsForTheSameMethodAreRejectedAtStartup() {
        PaymentProcessor secondCod = new CodPaymentProcessor(mock(PaymentRepository.class));

        assertThatThrownBy(() -> new PaymentProcessorRegistryImpl(List.of(cod, secondCod)))
            .isInstanceOf(IllegalStateException.class);
    }
}
