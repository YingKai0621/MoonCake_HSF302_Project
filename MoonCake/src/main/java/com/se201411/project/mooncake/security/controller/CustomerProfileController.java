package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.CustomerProfileForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import jakarta.validation.Valid;
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
            model.addAttribute("profile", customerProfileService.requireProfile(customer));
            return "security/profile";
        }
        customerProfileService.updateFullName(customer, form.getFullName());
        flash.addFlashAttribute("successMessage", "Đã cập nhật họ tên.");
        return "redirect:/profile";
    }
}
