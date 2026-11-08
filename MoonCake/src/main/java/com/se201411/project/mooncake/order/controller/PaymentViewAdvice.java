package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PaymentViewAdvice {
    private final PaymentProcessorRegistry paymentProcessorRegistry;

    public PaymentViewAdvice(PaymentProcessorRegistry paymentProcessorRegistry) {
        this.paymentProcessorRegistry = paymentProcessorRegistry;
    }

    @ModelAttribute("vnpayEnabled")
    public boolean vnpayEnabled() {
        return paymentProcessorRegistry.forMethod(PaymentMethod.VNPAY).isAvailable();
    }
}
