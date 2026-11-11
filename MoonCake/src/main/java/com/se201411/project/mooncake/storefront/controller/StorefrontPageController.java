package com.se201411.project.mooncake.storefront.controller;

import com.se201411.project.mooncake.storefront.service.StorefrontService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StorefrontPageController {

    private final StorefrontService storefrontService;

    public StorefrontPageController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    @GetMapping("/")
    public String homepage(Model model) {
        model.addAttribute("page", storefrontService.homePage());
        return "storefront/homepage";
    }

    @GetMapping("/business")
    public String business() {
        return "storefront/business";
    }
}
