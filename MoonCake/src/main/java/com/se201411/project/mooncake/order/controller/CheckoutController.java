package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.cart.dto.CheckoutLineQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutLineView;
import com.se201411.project.mooncake.order.dto.CheckoutPageView;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.dto.OrderSuccessView;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class CheckoutController {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

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
            CheckoutForm form = createDefaultForm(preparation);
            addCheckoutModel(model, preparation, form);
            return "checkout";
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
        RedirectAttributes redirectAttributes
    ) {
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

    private CheckoutForm createDefaultForm(CheckoutPreparation preparation) {
        CustomerProfile customer = preparation.customer();
        CheckoutForm form = new CheckoutForm();
        form.setCartId(preparation.cartId());
        form.setRecipientName(customer.fullName());
        form.setRecipientPhone(customer.phone());
        return form;
    }

    private void addCheckoutModel(
        Model model,
        CheckoutPreparation preparation,
        CheckoutForm form
    ) {
        model.addAttribute("checkoutForm", form);
        model.addAttribute("checkout", toPageView(preparation));
        model.addAttribute("customer", preparation.customer());
        model.addAttribute("provinces", deliveryAddressService.getProvinces());
        model.addAttribute("wards", deliveryAddressService.getWards());
    }

    private CheckoutPageView toPageView(CheckoutPreparation preparation) {
        CheckoutQuote quote = preparation.quote();
        List<CheckoutLineView> lineViews = new ArrayList<>();
        int totalQuantity = 0;

        for (CheckoutLineQuote line : quote.lines()) {
            totalQuantity = Math.addExact(totalQuantity, line.cartLine().quantity());
            lineViews.add(new CheckoutLineView(
                line.productName(),
                line.sku() == null ? line.boxSku() : line.sku(),
                line.cartLine().quantity(),
                formatVnd(line.unitBasePrice()),
                discountLabel(line.discountPercent()),
                formatVnd(line.lineTotal())
            ));
        }

        return new CheckoutPageView(
            preparation.cartId(),
            lineViews,
            totalQuantity,
            formatVnd(quote.subtotal()),
            formatVnd(quote.discount()),
            formatVnd(quote.shippingFee()),
            formatVnd(quote.total())
        );
    }

    private String discountLabel(BigDecimal discountPercent) {
        if (discountPercent.signum() == 0) {
            return null;
        }
        return "Giảm " + discountPercent.stripTrailingZeros().toPlainString() + "%";
    }

    private String firstError(BindingResult bindingResult) {
        if (bindingResult.getFieldError() == null) {
            return "Thông tin checkout không hợp lệ";
        }
        return bindingResult.getFieldError().getDefaultMessage();
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }
}
