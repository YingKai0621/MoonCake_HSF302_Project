package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.service.*;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminCatalogController {
    private final ProductAdminService catalog;
    private final PresetConfigurationAdminService presets;

    public AdminCatalogController(ProductAdminService catalog, PresetConfigurationAdminService presets) {
        this.catalog = catalog;
        this.presets = presets;
    }

    @GetMapping({"", "/"})
    public String home() {
        return "redirect:/admin/orders";
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) ProductType type, Model model) {
        model.addAttribute("products", catalog.productRows(type));
        model.addAttribute("tabs", catalog.productTabs(type));
        model.addAttribute("categories", catalog.categories());
        return "admin/catalog";
    }

    @GetMapping("/catalog/products/new")
    public String newProduct(Model model) {
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("productTypes", catalog.productTypes());
        return "admin/product-new";
    }

    @GetMapping("/catalog/products/{id}")
    public String editProduct(@PathVariable long id, Model model) {
        model.addAttribute("detail", catalog.productDetail(id));
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("productTypes", catalog.productTypes());
        return "admin/product-edit";
    }

    @PostMapping("/catalog/products/{id}/delete")
    public String deleteProduct(@PathVariable long id, RedirectAttributes flash) {
        String name = catalog.deleteProduct(id);
        flash.addFlashAttribute("success", "Đã xóa sản phẩm " + name + ".");
        return "redirect:/admin/catalog";
    }

    @PostMapping("/variants/{id}/delete")
    public String deleteVariant(@PathVariable long id, RedirectAttributes flash) {
        long productId = catalog.deleteVariant(id);
        flash.addFlashAttribute("success", "Đã xóa phiên bản.");
        return "redirect:/admin/catalog/products/" + productId + "#skus";
    }

    @PostMapping("/categories")
    public String category(@Valid @ModelAttribute CategoryForm form, RedirectAttributes flash) {
        catalog.saveCategory(form);
        return saved(flash, "catalog#categories");
    }

    @PostMapping("/products")
    public String product(@Valid @ModelAttribute ProductForm form, RedirectAttributes flash) {
        long productId = catalog.saveProduct(form);
        return savedProduct(flash, productId, "");
    }

    @PostMapping("/catalog/products")
    public String createProduct(@Valid @ModelAttribute NewProductForm form, RedirectAttributes flash) {
        long productId = catalog.createProduct(form);
        flash.addFlashAttribute("success", "Đã tạo sản phẩm. Bạn có thể thêm phiên bản và ảnh khác bên dưới.");
        return "redirect:/admin/catalog/products/" + productId;
    }

    @PostMapping("/variants")
    public String variant(@Valid @ModelAttribute VariantForm form, RedirectAttributes flash) {
        catalog.saveVariant(form);
        return savedProduct(flash, form.productId(), "#skus");
    }

    @PostMapping("/boxes")
    public String box(@Valid @ModelAttribute BoxForm form, RedirectAttributes flash) {
        long productId = catalog.saveBox(form);
        return savedProduct(flash, productId, "#boxes");
    }

    @PostMapping("/images")
    public String image(@Valid @ModelAttribute ImageForm form, RedirectAttributes flash) {
        long productId = catalog.saveImage(form);
        return savedProduct(flash, productId, "#sku-" + form.variantId());
    }

    @PostMapping("/images/upload")
    public String uploadImage(@Valid @ModelAttribute ImageUploadForm form, RedirectAttributes flash) {
        long productId = catalog.uploadImage(form);
        flash.addFlashAttribute("success", "Đã tải ảnh lên.");
        return "redirect:/admin/catalog/products/" + productId + "#sku-" + form.variantId();
    }

    @PostMapping("/images/{id}/remove")
    public String removeImage(@PathVariable long id, RedirectAttributes flash) {
        long productId = catalog.removeImage(id);
        flash.addFlashAttribute("success", "Đã xóa ảnh.");
        return "redirect:/admin/catalog/products/" + productId + "#skus";
    }

    @GetMapping("/presets")
    public String presets(Model model) {
        model.addAttribute("presets", presets.presets());
        model.addAttribute("catalog", catalog.catalog());
        return "admin/presets";
    }

    @PostMapping("/presets")
    public String preset(@Valid @ModelAttribute AdminPresetForm form, RedirectAttributes flash) {
        presets.saveDraft(form);
        return saved(flash, form.id() == null ? "presets" : "presets#preset-" + form.id());
    }

    @PostMapping("/presets/{id}/components")
    public String component(@PathVariable long id, @RequestParam long variantId, @RequestParam int quantity, RedirectAttributes flash) {
        presets.setComponent(id, variantId, quantity);
        return saved(flash, "presets#preset-" + id);
    }

    @PostMapping("/presets/{id}/activate")
    public String activate(@PathVariable long id, RedirectAttributes flash) {
        presets.activate(id);
        return saved(flash, "presets#preset-" + id);
    }

    private String savedProduct(RedirectAttributes flash, long productId, String anchor) {
        flash.addFlashAttribute("success", "Đã lưu thay đổi.");
        return "redirect:/admin/catalog/products/" + productId + anchor;
    }

    private String saved(RedirectAttributes flash, String path) {
        flash.addFlashAttribute("success", "Đã lưu thay đổi.");
        return "redirect:/admin/" + path;
    }
}
