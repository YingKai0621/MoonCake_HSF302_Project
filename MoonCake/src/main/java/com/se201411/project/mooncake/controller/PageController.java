package com.se201411.project.mooncake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for rendering the official MoonCake UI pages.
 * Maps the 6 customer-facing Thymeleaf templates for desktop navigation.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String homepage() {
        return "homepage";
    }

    @GetMapping("/products")
    public String shopCatalog() {
        return "shop-catalog";
    }

    @GetMapping("/products/demo")
    public String productDetail() {
        return "product-detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "checkout";
    }

    @GetMapping("/order-success")
    public String orderSuccess() {
        return "order-success";
    }
}
