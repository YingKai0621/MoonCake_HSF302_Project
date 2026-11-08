package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.enums.PaymentMethod;

/** Finds the PaymentProcessor that handles a payment method. */
public interface PaymentProcessorRegistry {

    PaymentProcessor forMethod(PaymentMethod method);
}
