package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.order.service.VnpayReconciliationService;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import jakarta.servlet.http.HttpServletRequest;
import com.se201411.project.mooncake.enums.OrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private final OrderManagementService orders;
    private final OrderCancellationService cancellations;
    private final VnpayReconciliationService vnpayReconciliation;

    public AdminOrderController(
        OrderManagementService orders,
        OrderCancellationService cancellations,
        VnpayReconciliationService vnpayReconciliation
    ) {
        this.orders = orders;
        this.cancellations = cancellations;
        this.vnpayReconciliation = vnpayReconciliation;
    }

    @GetMapping
    public String orders(@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", orders.orders(status, page));
        model.addAttribute("status", status);
        model.addAttribute("page", Math.max(0, page));
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("order", orders.detail(id));
        return "admin/order-detail";
    }

    @PostMapping("/{id}/status")
    public String transition(@PathVariable long id, @RequestParam OrderStatus target, RedirectAttributes flash) {
        if (target == OrderStatus.CANCELLED) {
            cancellations.cancelForAdmin(id);
        } else {
            orders.transition(id, target);
        }
        flash.addFlashAttribute("success", "Đã cập nhật đơn hàng.");
        return "redirect:/admin/orders/" + id;
    }

    /** Asks VNPay for the real result of the order's pending payment attempt. */
    @PostMapping("/{id}/payment-check")
    public String checkVnpayPayment(@PathVariable long id, HttpServletRequest request, RedirectAttributes flash) {
        try {
            String message = vnpayReconciliation.checkPaymentForAdmin(id, request.getRemoteAddr());
            flash.addFlashAttribute("success", message);
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("success", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
