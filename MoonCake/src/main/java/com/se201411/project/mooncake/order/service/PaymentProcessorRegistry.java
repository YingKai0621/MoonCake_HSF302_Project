package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.enums.PaymentMethod;

public interface PaymentProcessorRegistry {
    PaymentProcessor forMethod(PaymentMethod method);
}
