package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.security.dto.AdminAccountForm;
import com.se201411.project.mooncake.security.service.AdminAccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/admins")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public String admins(Model model) {
        model.addAttribute("page", adminAccountService.page());
        if (!model.containsAttribute("adminForm")) {
            model.addAttribute("adminForm", new AdminAccountForm());
        }
        return "admin/admins";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("adminForm") AdminAccountForm adminForm,
        BindingResult result,
        RedirectAttributes flash
    ) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("adminError", result.getAllErrors().get(0).getDefaultMessage());
            flash.addFlashAttribute("adminForm", withoutPassword(adminForm));
            return "redirect:/admin/admins";
        }
        try {
            adminAccountService.create(adminForm);
            flash.addFlashAttribute("success", "Đã thêm quản trị viên " + adminForm.getEmail().trim() + ".");
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("adminError", exception.getMessage());
            flash.addFlashAttribute("adminForm", withoutPassword(adminForm));
        }
        return "redirect:/admin/admins";
    }

    @PostMapping("/{accountId}/lock")
    public String lock(@PathVariable long accountId, RedirectAttributes flash) {
        try {
            adminAccountService.lock(accountId);
            flash.addFlashAttribute("success", "Đã khóa tài khoản quản trị.");
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping("/{accountId}/unlock")
    public String unlock(@PathVariable long accountId, RedirectAttributes flash) {
        try {
            adminAccountService.unlock(accountId);
            flash.addFlashAttribute("success", "Đã mở khóa tài khoản quản trị.");
        } catch (MoonCakeException exception) {
            flash.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/admins";
    }

    private AdminAccountForm withoutPassword(AdminAccountForm form) {
        AdminAccountForm copy = new AdminAccountForm();
        copy.setEmail(form.getEmail());
        return copy;
    }
}
