package com.se201411.project.mooncake.order.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import com.se201411.project.mooncake.common.web.FormErrors;
import com.se201411.project.mooncake.order.dto.ShipmentForm;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.enums.OrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private final OrderManagementService orders;
    private final OrderCancellationService cancellations;

    public AdminOrderController(
        OrderManagementService orders,
        OrderCancellationService cancellations
    ) {
        this.orders = orders;
        this.cancellations = cancellations;
    }

    @GetMapping
    public String orders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("page", orders.ordersPage(status, page));
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

    @PostMapping("/{id}/ship")
    public String ship(
        @PathVariable long id,
        @Valid @ModelAttribute ShipmentForm form,
        BindingResult result,
        RedirectAttributes flash
    ) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("shipError", FormErrors.first(result));
            return "redirect:/admin/orders/" + id;
        }
        try {
            orders.ship(id, form);
            flash.addFlashAttribute("success", "Đã bàn giao vận chuyển.");
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("shipError", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/payment-check")
    public String refreshPaymentStatus(@PathVariable long id, RedirectAttributes flash) {
        try {
            orders.refreshPaymentStatus(id);
            flash.addFlashAttribute("success", "Đã đối soát thanh toán với VNPay.");
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("success", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
