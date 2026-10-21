package com.se201411.project.mooncake.cart.controller;

import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CartHeaderViewAdvice {

    private final CustomerCartService customerCartService;
    private final CurrentCustomerService currentCustomerService;

    public CartHeaderViewAdvice(
        CustomerCartService customerCartService,
        CurrentCustomerService currentCustomerService
    ) {
        this.customerCartService = customerCartService;
        this.currentCustomerService = currentCustomerService;
    }

    @ModelAttribute
    public void addCartHeaderAttributes(Model model, Authentication authentication) {
        int cartQuantity = 0;
        if (isActiveCustomer(authentication)) {
            ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
            cartQuantity = customerCartService.countCurrentItems(customer);
        }
        model.addAttribute("headerCartQuantity", cartQuantity);
    }

    private boolean isActiveCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof MoonCakeUserPrincipal principal)) {
            return false;
        }
        return principal.isEnabled()
            && principal.getRole() == AccountRole.CUSTOMER
            && principal.getCustomerId() != null;
    }
}
