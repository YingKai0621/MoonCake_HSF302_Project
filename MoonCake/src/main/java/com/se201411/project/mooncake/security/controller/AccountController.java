package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.ChangePasswordForm;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.exception.SamePasswordException;
import com.se201411.project.mooncake.security.exception.WrongCurrentPasswordException;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import com.se201411.project.mooncake.security.util.PasswordRules;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequestMapping("/account")
public class AccountController {

    private static final String ACCOUNT_VIEW = "account";

    private final CurrentCustomerService currentCustomerService;
    private final CustomerProfileService customerProfileService;
    private final AuthenticationService authenticationService;

    public AccountController(
        CurrentCustomerService currentCustomerService,
        CustomerProfileService customerProfileService,
        AuthenticationService authenticationService
    ) {
        this.currentCustomerService = currentCustomerService;
        this.customerProfileService = customerProfileService;
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public String accountPage(Model model) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        return showAccountPage(model, customer);
    }

    @PostMapping("/profile")
    public String updateProfile(
        @Valid @ModelAttribute("profileForm") ProfileUpdateForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            return showAccountPage(model, customer);
        }

        customerProfileService.updateProfile(customer, form);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin cá nhân.");
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(
        @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue(
                "confirmPassword",
                "password.mismatch",
                "Mật khẩu xác nhận không khớp"
            );
        }
        if (PasswordRules.exceedsBcryptLimit(form.getNewPassword())) {
            bindingResult.rejectValue(
                "newPassword",
                "password.tooLongForBcrypt",
                "Mật khẩu chứa ký tự đặc biệt quá dài, vui lòng rút ngắn"
            );
        }
        if (bindingResult.hasErrors()) {
            return showAccountPage(model, customer);
        }

        try {
            authenticationService.changePassword(customer, form.getCurrentPassword(), form.getNewPassword());
        } catch (WrongCurrentPasswordException exception) {
            bindingResult.rejectValue("currentPassword", "password.wrong", exception.getMessage());
            return showAccountPage(model, customer);
        } catch (SamePasswordException exception) {
            bindingResult.rejectValue("newPassword", "password.same", exception.getMessage());
            return showAccountPage(model, customer);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đã đổi mật khẩu.");
        return "redirect:/account";
    }

    private String showAccountPage(Model model, ShopperIdentity customer) {
        model.addAttribute("page", customerProfileService.accountPage(customer));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", customerProfileService.profileForm(customer));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }
        return ACCOUNT_VIEW;
    }
}
