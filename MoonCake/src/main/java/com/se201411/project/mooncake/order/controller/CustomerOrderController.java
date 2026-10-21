package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final CurrentCustomerService currentCustomerService;

    public CustomerOrderController(
        CustomerOrderService customerOrderService,
        CurrentCustomerService currentCustomerService
    ) {
        this.customerOrderService = customerOrderService;
        this.currentCustomerService = currentCustomerService;
    }

    @GetMapping("/orders")
    public String showOrders(Model model) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        List<CustomerOrderSummaryView> orders = customerOrderService.findOrders(customer);
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/orders/{orderCode}")
    public String showOrderDetail(
        @PathVariable String orderCode,
        Model model
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        CustomerOrderDetailView order = customerOrderService.requireOrder(
            orderCode,
            customer
        );
        model.addAttribute("order", order);
        return "order-detail";
    }
}
