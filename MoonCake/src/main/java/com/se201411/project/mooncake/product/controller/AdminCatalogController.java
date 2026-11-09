package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.service.*;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.web.FormErrors;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindingResult;
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
        String name;
        try {
            name = catalog.deleteProduct(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(id), "danger", null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã xóa sản phẩm " + name + ".");
        return "redirect:/admin/catalog";
    }

    @PostMapping("/variants/{id}/delete")
    public String deleteVariant(@PathVariable long id, @RequestParam long productId, RedirectAttributes flash) {
        try {
            catalog.deleteVariant(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), "sku-" + id, null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã xóa phiên bản.");
        return "redirect:" + productPage(productId) + "#skus";
    }

    @PostMapping("/categories")
    public String category(@Valid @ModelAttribute CategoryForm form, BindingResult result, RedirectAttributes flash) {
        String target = form.id() == null ? "category-new" : "category-" + form.id();
        if (result.hasErrors()) {
            return failed(flash, "/admin/catalog", target, form, FormErrors.first(result));
        }
        try {
            catalog.saveCategory(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, "/admin/catalog", target, form, message(ex));
        }
        return saved(flash, "catalog#categories");
    }

    @PostMapping("/products")
    public String product(@Valid @ModelAttribute ProductForm form, BindingResult result, RedirectAttributes flash) {
        String page = "/admin/catalog/products/new";
        if (form.id() != null) {
            page = productPage(form.id());
        }
        if (result.hasErrors()) {
            return failed(flash, page, "info", form, FormErrors.first(result));
        }
        long productId;
        try {
            productId = catalog.saveProduct(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, page, "info", form, message(ex));
        }
        return savedProduct(flash, productId, "");
    }

    @PostMapping("/catalog/products")
    public String createProduct(@Valid @ModelAttribute NewProductForm form, BindingResult result, RedirectAttributes flash) {
        String newPage = "/admin/catalog/products/new";
        if (result.hasErrors()) {
            return failed(flash, newPage, "new-product", withoutFile(form), FormErrors.first(result));
        }
        long productId;
        try {
            productId = catalog.createProduct(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, newPage, "new-product", withoutFile(form), message(ex));
        }
        flash.addFlashAttribute("success", "Đã tạo sản phẩm. Bạn có thể thêm phiên bản và ảnh khác bên dưới.");
        return "redirect:" + productPage(productId);
    }

    @PostMapping("/variants")
    public String variant(@Valid @ModelAttribute VariantForm form, BindingResult result, RedirectAttributes flash) {
        String target = form.id() == null ? "sku-new" : "sku-" + form.id();
        if (result.hasErrors()) {
            return failed(flash, productPage(form.productId()), target, form, FormErrors.first(result));
        }
        try {
            catalog.saveVariant(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(form.productId()), target, form, message(ex));
        }
        return savedProduct(flash, form.productId(), "#" + target);
    }

    @PostMapping("/boxes")
    public String box(@Valid @ModelAttribute BoxForm form, BindingResult result,
                      @RequestParam long productId, RedirectAttributes flash) {
        String target = form.id() == null ? "box-new" : "box-" + form.id();
        if (result.hasErrors()) {
            return failed(flash, productPage(productId), target, form, FormErrors.first(result));
        }
        try {
            catalog.saveBox(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), target, form, message(ex));
        }
        return savedProduct(flash, productId, "#boxes");
    }

    @PostMapping("/images")
    public String image(@Valid @ModelAttribute ImageForm form, BindingResult result,
                        @RequestParam long productId, RedirectAttributes flash) {
        String target = "sku-" + form.variantId();
        if (result.hasErrors()) {
            return failed(flash, productPage(productId), target, null, FormErrors.first(result));
        }
        try {
            catalog.saveImage(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), target, null, message(ex));
        }
        return savedProduct(flash, productId, "#" + target);
    }

    @PostMapping("/images/upload")
    public String uploadImage(@Valid @ModelAttribute ImageUploadForm form, BindingResult result,
                              @RequestParam long productId, RedirectAttributes flash) {
        String target = "sku-" + form.variantId();
        if (result.hasErrors()) {
            return failed(flash, productPage(productId), target, null, FormErrors.first(result));
        }
        try {
            catalog.uploadImage(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), target, null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã tải ảnh lên.");
        return "redirect:" + productPage(productId) + "#" + target;
    }

    @PostMapping("/images/{id}/remove")
    public String removeImage(@PathVariable long id, @RequestParam long productId,
                              @RequestParam long variantId, RedirectAttributes flash) {
        String target = "sku-" + variantId;
        try {
            catalog.removeImage(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), target, null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã xóa ảnh.");
        return "redirect:" + productPage(productId) + "#" + target;
    }

    @GetMapping("/presets")
    public String presets(Model model) {
        model.addAttribute("presets", presets.presets());
        model.addAttribute("options", presets.presetOptions());
        return "admin/presets";
    }

    @PostMapping("/presets")
    public String preset(@Valid @ModelAttribute AdminPresetForm form, BindingResult result, RedirectAttributes flash) {
        String target = form.id() == null ? "preset-new" : "preset-" + form.id();
        if (result.hasErrors()) {
            return failed(flash, "/admin/presets", target, form, FormErrors.first(result));
        }
        try {
            presets.saveDraft(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, "/admin/presets", target, form, message(ex));
        }
        if (form.id() == null) {
            flash.addFlashAttribute("success", "Đã tạo công thức. Hãy thêm bánh cho đủ hộp.");
            return "redirect:/admin/presets";
        }
        return saved(flash, "presets#" + target);
    }

    @PostMapping("/presets/{id}/components")
    public String component(@PathVariable long id, @RequestParam long variantId, @RequestParam int quantity,
                            RedirectAttributes flash) {
        String target = "preset-" + id;
        try {
            presets.setComponent(id, variantId, quantity);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, "/admin/presets", target, null, message(ex));
        }
        return saved(flash, "presets#" + target);
    }

    @PostMapping("/presets/{id}/activate")
    public String activate(@PathVariable long id, RedirectAttributes flash) {
        String target = "preset-" + id;
        try {
            presets.activate(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, "/admin/presets", target, null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã khóa công thức. Giờ có thể đóng hộp ở trang Tồn kho.");
        return "redirect:/admin/presets#" + target;
    }

    private String failed(RedirectAttributes flash, String page, String target, Object values, String message) {
        flash.addFlashAttribute("formTarget", target);
        flash.addFlashAttribute("formError", message);
        if (values != null) {
            flash.addFlashAttribute("formValues", values);
        }
        return "redirect:" + page + "#" + target;
    }

    private String message(Exception ex) {
        if (ex instanceof MoonCakeException) {
            return ex.getMessage();
        }
        return "Không lưu được vì dữ liệu bị trùng hoặc vừa được thay đổi. Hãy tải lại trang và thử lại.";
    }

    private String productPage(long productId) {
        return "/admin/catalog/products/" + productId;
    }

    private NewProductForm withoutFile(NewProductForm form) {
        return new NewProductForm(
            form.categoryId(),
            form.productType(),
            form.name(),
            form.slug(),
            form.shortDescription(),
            form.detailedDescription(),
            form.mainIngredients(),
            form.shelfLifeInfo(),
            form.storageGuidance(),
            form.active(),
            form.sku(),
            form.variantName(),
            form.unitPrice(),
            form.weightGrams(),
            form.fillingLabel(),
            form.imageUrl(),
            null
        );
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
