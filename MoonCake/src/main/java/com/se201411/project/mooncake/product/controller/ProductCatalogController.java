package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.product.service.ProductQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
public class ProductCatalogController {

    private final ProductQueryService productQueryService;

    public ProductCatalogController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping
    public String catalogRedirect() {
        return "redirect:/products/gift-boxes";
    }

    @GetMapping("/demo")
    public String legacyDemoRedirect() {
        return "redirect:/products/gift-boxes";
    }

    @GetMapping("/gift-boxes")
    public String giftBoxesCatalog(Model model) {
        model.addAttribute("page", productQueryService.giftBoxCatalogPage());
        return "product/shop-catalog";
    }

    @GetMapping("/mooncakes")
    public String mooncakesCatalog(Model model) {
        model.addAttribute("page", productQueryService.mooncakeCatalogPage());
        return "product/shop-catalog";
    }

    @GetMapping("/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        model.addAttribute("page", productQueryService.productDetailPage(slug));
        return "product/product-detail";
    }
}
