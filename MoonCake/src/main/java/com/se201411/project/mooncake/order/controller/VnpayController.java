package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class VnpayController {
    private final CustomerOrderService customerOrderService;
    private final CurrentCustomerService currentCustomerService;
    private final PaymentProcessorRegistry paymentProcessorRegistry;

    public VnpayController(
        CustomerOrderService customerOrderService,
        CurrentCustomerService currentCustomerService,
        PaymentProcessorRegistry paymentProcessorRegistry
    ) {
        this.customerOrderService = customerOrderService;
        this.currentCustomerService = currentCustomerService;
        this.paymentProcessorRegistry = paymentProcessorRegistry;
    }

    @PostMapping("/orders/{code}/pay-vnpay")
    public String pay(
        @PathVariable String code,
        HttpServletRequest request,
        RedirectAttributes flash
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            String paymentUrl = customerOrderService.startOnlinePayment(
                code,
                customer,
                request.getRemoteAddr()
            );
            return "redirect:" + paymentUrl;
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/orders/" + code;
        }
    }

    @GetMapping("/payments/vnpay/ipn")
    @ResponseBody
    public Map<String, String> ipn(
        @RequestParam Map<String, String> params,
        HttpServletRequest request
    ) {
        for (String[] values : request.getParameterMap().values()) {
            if (values.length != 1) {
                return Map.of("RspCode", "97", "Message", "Duplicate parameters");
            }
        }
        return vnpay().handleNotification(params);
    }

    @GetMapping("/payments/vnpay/return")
    public String returnFromVnpay(
        @RequestParam Map<String, String> params,
        RedirectAttributes flash
    ) {
        if (vnpay().verifyReturn(params)) {
            flash.addFlashAttribute(
                "successMessage",
                "Đã trở về từ VNPay. Trạng thái thanh toán được cập nhật sau xác nhận của VNPay; hãy tải lại đơn hàng."
            );
        } else {
            flash.addFlashAttribute("successMessage", "Không xác minh được kết quả VNPay.");
        }
        return "redirect:/orders";
    }

    private PaymentProcessor vnpay() {
        return paymentProcessorRegistry.forMethod(PaymentMethod.VNPAY);
    }
}
