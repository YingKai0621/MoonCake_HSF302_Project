package com.se201411.project.mooncake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Temporary controller for pages whose feature-specific backend is not implemented yet.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String homepage() {
        return "homepage";
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
