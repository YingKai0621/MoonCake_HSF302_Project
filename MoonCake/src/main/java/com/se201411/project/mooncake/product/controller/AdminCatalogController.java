package com.se201411.project.mooncake.product.controller;

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
    public String catalog(Model model) {
        model.addAttribute("catalog", catalog.catalog());
        return "admin/catalog";
    }

    @PostMapping("/categories")
    public String category(@Valid @ModelAttribute CategoryForm form, RedirectAttributes flash) {
        catalog.saveCategory(form);
        return saved(flash, "catalog#categories");
    }

    @PostMapping("/products")
    public String product(@Valid @ModelAttribute ProductForm form, RedirectAttributes flash) {
        catalog.saveProduct(form);
        return saved(flash, "catalog#products");
    }

    @PostMapping("/variants")
    public String variant(@Valid @ModelAttribute VariantForm form, RedirectAttributes flash) {
        catalog.saveVariant(form);
        return saved(flash, "catalog#variants");
    }

    @PostMapping("/boxes")
    public String box(@Valid @ModelAttribute BoxForm form, RedirectAttributes flash) {
        catalog.saveBox(form);
        return saved(flash, "catalog#boxes");
    }

    @PostMapping("/images")
    public String image(@Valid @ModelAttribute ImageForm form, RedirectAttributes flash) {
        catalog.saveImage(form);
        return saved(flash, "catalog#images");
    }

    @PostMapping("/images/{id}/remove")
    public String removeImage(@PathVariable long id, RedirectAttributes flash) {
        catalog.removeImage(id);
        return saved(flash, "catalog#images");
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

    private String saved(RedirectAttributes flash, String path) {
        flash.addFlashAttribute("success", "Đã lưu thành công.");
        return "redirect:/admin/" + path;
    }
}
