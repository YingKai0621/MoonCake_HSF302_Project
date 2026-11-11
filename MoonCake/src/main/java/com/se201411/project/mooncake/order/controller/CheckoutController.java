package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.order.dto.CheckoutPageView;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.dto.OrderSuccessView;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class CheckoutController {
    private final CheckoutService checkoutService;
    private final CurrentCustomerService currentCustomerService;
    private final DeliveryAddressService deliveryAddressService;

    public CheckoutController(
        CheckoutService checkoutService,
        CurrentCustomerService currentCustomerService,
        DeliveryAddressService deliveryAddressService
    ) {
        this.checkoutService = checkoutService;
        this.currentCustomerService = currentCustomerService;
        this.deliveryAddressService = deliveryAddressService;
    }

    @GetMapping("/checkout")
    public String showCheckout(
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
            CheckoutPreparation preparation = checkoutService.prepareCheckout(customer);
            CheckoutPageView checkout = checkoutService.checkoutPage(preparation);
            CheckoutForm form = createDefaultForm(preparation, checkout);
            model.addAttribute("checkoutForm", form);
            model.addAttribute("checkout", checkout);
            return "checkout";
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Giỏ hàng của bạn đã được đặt thành đơn. Bạn có thể theo dõi hoặc thanh toán đơn ở danh sách bên dưới."
            );
            return "redirect:/orders";
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping("/checkout")
    public String placeOrder(
        @Valid @ModelAttribute("checkoutForm") CheckoutForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) {
        form.setPaymentIp(request.getRemoteAddr());
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            return showInvalidForm(customer, form, bindingResult, model, redirectAttributes);
        }

        try {
            deliveryAddressService.buildShippingAddress(
                form.getStreetAddress(),
                form.getWardCode(),
                form.getProvinceCode()
            );
        } catch (MoonCakeException exception) {
            bindingResult.rejectValue(
                "wardCode",
                "address.invalid",
                exception.getMessage()
            );
            return showInvalidForm(customer, form, bindingResult, model, redirectAttributes);
        }

        try {
            CheckoutResult result = checkoutService.placeOrder(customer, form);
            if (result.needsPaymentRedirect()) {
                return "redirect:" + result.paymentUrl();
            }
            return "redirect:/orders/" + result.orderCode() + "/success";
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders/{orderCode}/success")
    public String showOrderSuccess(
        @PathVariable String orderCode,
        Model model
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        OrderSuccessView order = checkoutService.requireOrder(orderCode, customer);
        model.addAttribute("order", order);
        return "order-success";
    }

    private String showInvalidForm(
        ShopperIdentity customer,
        CheckoutForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (form.getCartId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/cart";
        }

        try {
            CheckoutPreparation preparation = checkoutService.prepareCheckout(
                form.getCartId(),
                customer
            );
            addCheckoutModel(model, preparation, form);
            return "checkout";
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/cart";
        }
    }

    private CheckoutForm createDefaultForm(CheckoutPreparation preparation, CheckoutPageView checkout) {
        CustomerProfile customer = preparation.customer();
        CheckoutForm form = new CheckoutForm();
        form.setCartId(preparation.cartId());
        form.setRecipientName(customer.fullName());
        form.setRecipientPhone(customer.phone());
        form.setPaymentMethod(checkout.defaultPaymentMethod());
        return form;
    }

    private void addCheckoutModel(
        Model model,
        CheckoutPreparation preparation,
        CheckoutForm form
    ) {
        model.addAttribute("checkoutForm", form);
        model.addAttribute("checkout", checkoutService.checkoutPage(preparation));
    }

    private String firstError(BindingResult bindingResult) {
        if (bindingResult.getFieldError() == null) {
            return "Thông tin checkout không hợp lệ";
        }
        return bindingResult.getFieldError().getDefaultMessage();
    }
}
