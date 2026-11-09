package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.dto.PaymentReturnResult;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
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
    private final OrderCancellationService orderCancellationService;

    public VnpayController(
        CustomerOrderService customerOrderService,
        CurrentCustomerService currentCustomerService,
        PaymentProcessorRegistry paymentProcessorRegistry,
        OrderCancellationService orderCancellationService
    ) {
        this.customerOrderService = customerOrderService;
        this.currentCustomerService = currentCustomerService;
        this.paymentProcessorRegistry = paymentProcessorRegistry;
        this.orderCancellationService = orderCancellationService;
    }

    @PostMapping("/orders/{code}/pay-vnpay")
    public String pay(
        @PathVariable String code,
        HttpServletRequest request,
        RedirectAttributes flash
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            if (orderCancellationService.cancelOwnOrderIfPaymentExpired(code)) {
                flash.addFlashAttribute(
                    "errorMessage",
                    "Đơn hàng đã quá thời gian thanh toán nên đã được hủy."
                );
                return "redirect:/orders/" + code;
            }
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
        PaymentReturnResult result = vnpay().handleReturn(params);
        if (!result.verified()) {
            flash.addFlashAttribute("errorMessage", "Không xác minh được kết quả thanh toán. Vui lòng kiểm tra lại đơn hàng.");
            return "redirect:/orders";
        }
        if (result.paid()) {
            flash.addFlashAttribute("successMessage", "Thanh toán thành công. Cửa hàng sẽ sớm chuẩn bị đơn hàng của bạn.");
            flash.addFlashAttribute("celebrate", true);
        } else if (result.completedOnGateway()) {
            flash.addFlashAttribute("successMessage", "Đang chờ VNPay xác nhận thanh toán. Vui lòng tải lại trang sau ít phút.");
        } else {
            flash.addFlashAttribute("errorMessage", "Bạn chưa hoàn tất thanh toán. Bạn có thể thanh toán lại hoặc hủy đơn hàng.");
        }
        return "redirect:/orders/" + result.orderCode();
    }

    private PaymentProcessor vnpay() {
        return paymentProcessorRegistry.forMethod(PaymentMethod.VNPAY);
    }
}
