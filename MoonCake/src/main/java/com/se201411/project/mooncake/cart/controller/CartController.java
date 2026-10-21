package com.se201411.project.mooncake.cart.controller;

import com.se201411.project.mooncake.cart.dto.AddCartItemForm;
import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.dto.UpdateCartItemForm;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CustomerCartService customerCartService;
    private final CurrentCustomerService currentCustomerService;

    public CartController(
        CustomerCartService customerCartService,
        CurrentCustomerService currentCustomerService
    ) {
        this.customerCartService = customerCartService;
        this.currentCustomerService = currentCustomerService;
    }

    @GetMapping
    public String cart(Model model) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        CustomerCartView cart = customerCartService.getCurrentCart(customer);
        model.addAttribute("cart", cart);
        return "cart";
    }

    @GetMapping("/continue")
    public String continueAfterLogin(
        @RequestParam(required = false) String returnUrl
    ) {
        currentCustomerService.requireCurrentCustomer();
        return "redirect:" + safeProductReturnUrl(returnUrl);
    }

    @PostMapping("/items")
    public String addItem(
        @Valid @ModelAttribute AddCartItemForm addCartItemForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addValidationError(bindingResult, redirectAttributes);
            return "redirect:/cart";
        }

        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            customerCartService.addVariant(
                customer,
                addCartItemForm.getVariantId(),
                addCartItemForm.getQuantity()
            );
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã thêm sản phẩm vào giỏ hàng"
            );
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/items/{cartItemId}/quantity")
    public String updateItemQuantity(
        @PathVariable long cartItemId,
        @Valid @ModelAttribute UpdateCartItemForm updateCartItemForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addValidationError(bindingResult, redirectAttributes);
            return "redirect:/cart";
        }

        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            customerCartService.updateItemQuantity(
                customer,
                cartItemId,
                updateCartItemForm.getQuantity()
            );
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã cập nhật số lượng"
            );
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/items/{cartItemId}/delete")
    public String removeItem(
        @PathVariable long cartItemId,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            customerCartService.removeItem(customer, cartItemId);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã xóa sản phẩm khỏi giỏ hàng"
            );
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }

    private void addValidationError(
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        String message = "Dữ liệu giỏ hàng không hợp lệ";
        if (!bindingResult.getAllErrors().isEmpty()) {
            String validationMessage = bindingResult.getAllErrors().getFirst().getDefaultMessage();
            if (validationMessage != null && !validationMessage.isBlank()) {
                message = validationMessage;
            }
        }
        redirectAttributes.addFlashAttribute("errorMessage", message);
    }

    private String safeProductReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return "/products";
        }

        String trimmedUrl = returnUrl.trim();
        boolean productPage = trimmedUrl.startsWith("/products/");
        boolean externalUrl = trimmedUrl.startsWith("//")
            || trimmedUrl.contains("://")
            || trimmedUrl.contains("\r")
            || trimmedUrl.contains("\n");
        if (!productPage || externalUrl) {
            return "/products";
        }
        return trimmedUrl;
    }
}
