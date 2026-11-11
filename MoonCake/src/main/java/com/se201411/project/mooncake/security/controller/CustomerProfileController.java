package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        return "security/profile";
    }
}
