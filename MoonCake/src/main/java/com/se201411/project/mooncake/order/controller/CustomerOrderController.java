package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.dto.OrderDeliveryUpdateForm;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
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

import java.util.List;

@Controller
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final CurrentCustomerService currentCustomerService;
    private final OrderCancellationService orderCancellationService;
    private final DeliveryAddressService deliveryAddressService;

    public CustomerOrderController(
        CustomerOrderService customerOrderService,
        CurrentCustomerService currentCustomerService,
        OrderCancellationService orderCancellationService,
        DeliveryAddressService deliveryAddressService
    ) {
        this.customerOrderService = customerOrderService;
        this.currentCustomerService = currentCustomerService;
        this.orderCancellationService = orderCancellationService;
        this.deliveryAddressService = deliveryAddressService;
    }

    @GetMapping("/orders")
    public String showOrders(Model model) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        List<CustomerOrderSummaryView> orders = customerOrderService.findOrders(customer);
        model.addAttribute("orders", orders);
        return "order/orders";
    }

    @GetMapping("/orders/{orderCode}")
    public String showOrderDetail(
        @PathVariable String orderCode,
        Model model
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            orderCancellationService.cancelOwnOrderIfPaymentExpired(orderCode);
        } catch (MoonCakeException exception) {
            model.addAttribute(
                "errorMessage",
                "Chưa kiểm tra được thanh toán với VNPay. Vui lòng tải lại trang sau ít phút."
            );
        }
        CustomerOrderDetailView order = customerOrderService.requireOrder(
            orderCode,
            customer
        );
        model.addAttribute("order", order);
        return "order/order-detail";
    }

    @PostMapping("/orders/{orderCode}/cancel")
    public String cancelOrder(
        @PathVariable String orderCode,
        RedirectAttributes redirectAttributes
    ) {
        try {
            orderCancellationService.cancelCurrentCustomerOrder(orderCode);
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/orders/" + orderCode;
        }
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Đã hủy đơn hàng thành công."
        );
        return "redirect:/orders/" + orderCode;
    }

    @PostMapping("/orders/{orderCode}/received")
    public String confirmReceived(
        @PathVariable String orderCode,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        try {
            customerOrderService.confirmReceived(orderCode, customer);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Cảm ơn bạn đã xác nhận nhận hàng."
            );
        } catch (MoonCakeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/orders/" + orderCode;
    }

    @GetMapping("/orders/{orderCode}/delivery/edit")
    public String editDeliveryInformation(
        @PathVariable String orderCode,
        Model model
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        OrderDeliveryUpdateForm form = customerOrderService
            .requireDeliveryUpdateForm(orderCode, customer);
        addDeliveryFormModel(model, orderCode, form);
        return "order/order-delivery-edit";
    }

    @PostMapping("/orders/{orderCode}/delivery")
    public String updateDeliveryInformation(
        @PathVariable String orderCode,
        @Valid @ModelAttribute("deliveryForm") OrderDeliveryUpdateForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            return showInvalidDeliveryForm(
                orderCode,
                customer,
                form,
                model
            );
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
            return showInvalidDeliveryForm(
                orderCode,
                customer,
                form,
                model
            );
        }

        try {
            customerOrderService.updateDeliveryInformation(
                orderCode,
                form,
                customer
            );
        } catch (BusinessRuleViolationException | ConcurrentOperationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/orders/" + orderCode;
        }

        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Đã cập nhật thông tin nhận hàng."
        );
        return "redirect:/orders/" + orderCode;
    }

    private String showInvalidDeliveryForm(
        String orderCode,
        ShopperIdentity customer,
        OrderDeliveryUpdateForm form,
        Model model
    ) {
        OrderDeliveryUpdateForm currentForm = customerOrderService
            .requireDeliveryUpdateForm(orderCode, customer);
        form.setCurrentShippingAddress(currentForm.getCurrentShippingAddress());
        addDeliveryFormModel(model, orderCode, form);
        return "order/order-delivery-edit";
    }

    private void addDeliveryFormModel(
        Model model,
        String orderCode,
        OrderDeliveryUpdateForm form
    ) {
        model.addAttribute("deliveryForm", form);
        model.addAttribute("page", deliveryAddressService.deliveryEditPage(orderCode));
    }
}
