package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.CustomerEmailForm;
import com.se201411.project.mooncake.security.dto.CustomerPhoneForm;
import com.se201411.project.mooncake.security.dto.CustomerProfileForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerProfileController {

    private final CurrentCustomerService currentCustomerService;
    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(
        CurrentCustomerService currentCustomerService,
        CustomerProfileService customerProfileService
    ) {
        this.currentCustomerService = currentCustomerService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        CustomerProfile profile = customerProfileService.requireProfile(customer);
        model.addAttribute("profile", profile);
        CustomerProfileForm form = new CustomerProfileForm();
        form.setFullName(profile.fullName());
        model.addAttribute("profileForm", form);
        CustomerEmailForm emailForm = new CustomerEmailForm();
        emailForm.setEmail(profile.email());
        model.addAttribute("emailForm", emailForm);
        CustomerPhoneForm phoneForm = new CustomerPhoneForm();
        phoneForm.setPhone(profile.phone());
        model.addAttribute("phoneForm", phoneForm);
        return "security/profile";
    }

    @PostMapping("/profile/name")
    public String updateFullName(
        @Valid @ModelAttribute("profileForm") CustomerProfileForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes flash
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            addCurrentProfile(model, customer, null);
            return "security/profile";
        }
        customerProfileService.updateFullName(customer, form.getFullName());
        flash.addFlashAttribute("successMessage", "Đã cập nhật họ tên.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/email")
    public String updateEmail(
        @Valid @ModelAttribute("emailForm") CustomerEmailForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes flash,
        HttpServletRequest request
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            addCurrentProfile(model, customer, form);
            return "security/profile";
        }
        try {
            customerProfileService.updateEmail(customer, form.getEmail());
        } catch (EmailAlreadyRegisteredException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            addCurrentProfile(model, customer, form);
            return "security/profile";
        }
        new SecurityContextLogoutHandler().logout(request, null, null);
        flash.addFlashAttribute("emailChanged", true);
        return "redirect:/login?emailChanged";
    }

    @PostMapping("/profile/phone")
    public String updatePhone(
        @Valid @ModelAttribute("phoneForm") CustomerPhoneForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes flash
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            addCurrentProfile(model, customer, null);
            model.addAttribute("phoneForm", form);
            return "security/profile";
        }
        customerProfileService.updatePhone(customer, form.getPhone());
        flash.addFlashAttribute("successMessage", "Đã cập nhật số điện thoại.");
        return "redirect:/profile";
    }

    private void addCurrentProfile(Model model, ShopperIdentity customer, CustomerEmailForm emailForm) {
        CustomerProfile profile = customerProfileService.requireProfile(customer);
        model.addAttribute("profile", profile);
        CustomerProfileForm profileForm = new CustomerProfileForm();
        profileForm.setFullName(profile.fullName());
        model.addAttribute("profileForm", profileForm);
        if (emailForm == null) {
            emailForm = new CustomerEmailForm();
            emailForm.setEmail(profile.email());
        }
        model.addAttribute("emailForm", emailForm);
        CustomerPhoneForm phoneForm = new CustomerPhoneForm();
        phoneForm.setPhone(profile.phone());
        model.addAttribute("phoneForm", phoneForm);
    }
}
