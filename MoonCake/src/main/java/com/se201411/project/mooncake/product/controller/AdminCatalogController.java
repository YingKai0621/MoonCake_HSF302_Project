package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.product.dto.AdminPresetForm;
import com.se201411.project.mooncake.product.dto.ProductEditPageView;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.BoxForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.CategoryForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ImageForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ImageUploadForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.NewProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.web.FormErrors;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return "redirect:/admin/overview";
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) ProductType type, Model model) {
        if (type == ProductType.GIFT_SET) {
            return "redirect:/admin/gift-boxes";
        }
        model.addAttribute("page", catalog.catalogPage(type));
        return "admin/catalog";
    }

    @GetMapping("/catalog/products/new")
    public String newProduct(Model model) {
        model.addAttribute("page", catalog.newProductPage(false));
        return "admin/product-new";
    }

    @GetMapping("/catalog/products/{id}")
    public String editProduct(@PathVariable long id, Model model) {
        ProductEditPageView page = catalog.productEditPage(id);
        if (page.detail().product().productType() == ProductType.GIFT_SET) {
            return "redirect:/admin/gift-boxes/" + id;
        }
        model.addAttribute("page", page);
        return "admin/product-edit";
    }

    @PostMapping("/catalog/products/{id}/delete")
    public String deleteProduct(@PathVariable long id, @RequestParam(defaultValue = "false") boolean giftBox,
                                RedirectAttributes flash) {
        String name;
        try {
            name = catalog.deleteProduct(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(id), "danger", null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã xóa " + name + ".");
        if (giftBox) {
            return "redirect:/admin/gift-boxes";
        }
        return "redirect:/admin/catalog";
    }

    @PostMapping("/catalog/products/{id}/visibility")
    public String visibility(@PathVariable long id, @RequestParam boolean visible, RedirectAttributes flash) {
        try {
            catalog.setProductVisible(id, visible);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(id), "sale", null, message(ex));
        }
        if (visible) {
            flash.addFlashAttribute("success", "Sản phẩm đã hiện trên cửa hàng.");
        } else {
            flash.addFlashAttribute("success", "Đã ẩn sản phẩm khỏi cửa hàng.");
        }
        return "redirect:" + productPage(id);
    }

    @PostMapping("/variants/{id}/sale")
    public String variantSale(@PathVariable long id, @RequestParam boolean onSale,
                              @RequestParam long productId, RedirectAttributes flash) {
        try {
            catalog.setVariantOnSale(id, onSale);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), "sku-" + id, null, message(ex));
        }
        if (onSale) {
            flash.addFlashAttribute("success", "Đã mở bán phiên bản.");
        } else {
            flash.addFlashAttribute("success", "Đã ngừng bán phiên bản.");
        }
        return "redirect:" + productPage(productId) + "#sku-" + id;
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
        if (form.productType() == ProductType.GIFT_SET) {
            newPage = "/admin/gift-boxes/new";
        }
        if (result.hasErrors()) {
            return failed(flash, newPage, "new-product", withoutFile(form), FormErrors.first(result));
        }
        long productId;
        try {
            productId = catalog.createProduct(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, newPage, "new-product", withoutFile(form), message(ex));
        }
        if (form.productType() == ProductType.GIFT_SET) {
            flash.addFlashAttribute("success", "Đã tạo hộp quà. Bước tiếp theo: chọn bánh cho hộp.");
            return "redirect:/admin/gift-boxes/" + productId + "#recipe";
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
    public String presets() {
        return "redirect:/admin/gift-boxes";
    }

    @PostMapping("/presets")
    public String preset(@Valid @ModelAttribute AdminPresetForm form, BindingResult result,
                         @RequestParam long productId, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return failed(flash, productPage(productId), "recipe", form, FormErrors.first(result));
        }
        try {
            presets.saveDraft(form);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), "recipe", form, message(ex));
        }
        return savedProduct(flash, productId, "#recipe");
    }

    @PostMapping("/presets/{id}/activate")
    public String activate(@PathVariable long id, @RequestParam long productId, RedirectAttributes flash) {
        try {
            presets.activate(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, productPage(productId), "recipe", null, message(ex));
        }
        flash.addFlashAttribute("success", "Đã khóa danh sách bánh. Bước tiếp theo: đóng hộp ở trang Tồn kho.");
        return "redirect:" + productPage(productId) + "#recipe";
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
            form.shelfLifeDays(),
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
