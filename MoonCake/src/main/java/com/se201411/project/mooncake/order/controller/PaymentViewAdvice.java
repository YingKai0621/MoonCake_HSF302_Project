package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Lets every page know whether the VNPay option should be shown. */
@ControllerAdvice
public class PaymentViewAdvice {

    private final VnpayPaymentService vnpayPaymentService;

    public PaymentViewAdvice(VnpayPaymentService vnpayPaymentService) {
        this.vnpayPaymentService = vnpayPaymentService;
    }

    @ModelAttribute("vnpayEnabled")
    public boolean vnpayEnabled() {
        return vnpayPaymentService.isEnabled();
    }
}
