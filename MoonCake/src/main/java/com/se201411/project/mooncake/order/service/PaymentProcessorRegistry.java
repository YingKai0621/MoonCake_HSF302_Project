package com.se201411.project.mooncake.order.service;

import java.util.List;
import java.math.BigDecimal;
import com.se201411.project.mooncake.enums.PaymentMethod;

public interface PaymentProcessorRegistry {
    PaymentProcessor forMethod(PaymentMethod method);

    List<PaymentMethod> methodsAccepting(BigDecimal total);

    PaymentMethod defaultMethodFor(BigDecimal total);
}
