package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.security.dto.AccountLockForm;
import com.se201411.project.mooncake.security.dto.AccountLockResult;
import com.se201411.project.mooncake.security.service.CustomerAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerAdminService customerAdminService;

    public AdminCustomerController(CustomerAdminService customerAdminService) {
        this.customerAdminService = customerAdminService;
    }

    @GetMapping
    public String customers(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("page", customerAdminService.customersPage(q, page));
        return "admin/customers";
    }

    @GetMapping("/{customerId}")
    public String customer(@PathVariable long customerId, Model model) {
        model.addAttribute("page", customerAdminService.customerPage(customerId));
        if (!model.containsAttribute("lockForm")) {
            model.addAttribute("lockForm", new AccountLockForm());
        }
        return "admin/customer-detail";
    }

    @PostMapping("/{customerId}/lock")
    public String lock(
        @PathVariable long customerId,
        @ModelAttribute AccountLockForm lockForm,
        RedirectAttributes flash
    ) {
        try {
            AccountLockResult result = customerAdminService.lock(customerId, lockForm.isCancelUnpaidOrders());
            flash.addFlashAttribute("success", result.message());
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("lockError", exception.getMessage());
        }
        return "redirect:/admin/customers/" + customerId;
    }

    @PostMapping("/{customerId}/unlock")
    public String unlock(@PathVariable long customerId, RedirectAttributes flash) {
        try {
            customerAdminService.unlock(customerId);
            flash.addFlashAttribute("success", "Đã mở khóa tài khoản.");
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("lockError", exception.getMessage());
        }
        return "redirect:/admin/customers/" + customerId;
    }
}
