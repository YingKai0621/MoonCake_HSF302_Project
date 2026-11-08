package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.order.service.VnpayReconciliationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentCheckController {

    private final VnpayReconciliationService reconciliationService;

    public PaymentCheckController(VnpayReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/orders/{code}/payment-check")
    public String check(
        @PathVariable String code,
        HttpServletRequest request,
        RedirectAttributes flash
    ) {
        try {
            String message = reconciliationService.checkPayment(code, request.getRemoteAddr());
            flash.addFlashAttribute("successMessage", message);
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/orders/" + code;
    }
}
