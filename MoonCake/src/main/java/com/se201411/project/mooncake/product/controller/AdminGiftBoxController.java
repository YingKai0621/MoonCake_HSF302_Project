package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.product.dto.RecipeCakesForm;
import com.se201411.project.mooncake.product.service.GiftBoxAdminService;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import org.springframework.dao.DataAccessException;
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
@RequestMapping("/admin/gift-boxes")
public class AdminGiftBoxController {
    private final GiftBoxAdminService giftBoxes;
    private final ProductAdminService catalog;
    private final PresetConfigurationAdminService recipes;

    public AdminGiftBoxController(GiftBoxAdminService giftBoxes, ProductAdminService catalog,
                                  PresetConfigurationAdminService recipes) {
        this.giftBoxes = giftBoxes;
        this.catalog = catalog;
        this.recipes = recipes;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("giftBoxes", giftBoxes.giftBoxes());
        return "admin/gift-boxes";
    }

    @GetMapping("/new")
    public String newGiftBox(Model model) {
        model.addAttribute("page", catalog.newProductPage(true));
        return "admin/product-new";
    }

    @GetMapping("/{id}")
    public String page(@PathVariable long id, Model model) {
        model.addAttribute("page", giftBoxes.page(id));
        return "admin/gift-box";
    }

    @PostMapping("/{id}/sale")
    public String sale(@PathVariable long id, @RequestParam(defaultValue = "false") boolean onSale,
                       RedirectAttributes flash) {
        try {
            giftBoxes.setOnSale(id, onSale);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, id, "sale", ex);
        }
        if (onSale) {
            flash.addFlashAttribute("success", "Hộp quà đã được mở bán trên cửa hàng.");
        } else {
            flash.addFlashAttribute("success", "Đã ngừng bán hộp quà.");
        }
        return "redirect:/admin/gift-boxes/" + id + "#sale";
    }

    @PostMapping("/{id}/cakes")
    public String cakes(@PathVariable long id, @RequestParam long recipeId,
                        @RequestParam(defaultValue = "false") boolean lock,
                        @ModelAttribute RecipeCakesForm form, RedirectAttributes flash) {
        try {
            recipes.setComponents(recipeId, form.getQuantities());
            if (lock) {
                recipes.activate(recipeId);
            }
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, id, "recipe", ex);
        }
        if (lock) {
            flash.addFlashAttribute("success", "Đã khóa danh sách bánh. Bước tiếp theo: đóng hộp và mở bán.");
            return "redirect:/admin/gift-boxes/" + id + "#sale";
        }
        flash.addFlashAttribute("success", "Đã lưu nháp danh sách bánh.");
        return "redirect:/admin/gift-boxes/" + id + "#recipe";
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable long id, @RequestParam long recipeId, RedirectAttributes flash) {
        try {
            recipes.unlock(recipeId);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, id, "recipe", ex);
        }
        flash.addFlashAttribute("success", "Đã mở khóa. Bạn có thể sửa danh sách bánh.");
        return "redirect:/admin/gift-boxes/" + id + "#recipe";
    }

    @PostMapping("/{id}/copy")
    public String copy(@PathVariable long id, RedirectAttributes flash) {
        long copyId;
        try {
            copyId = giftBoxes.copy(id);
        } catch (MoonCakeException | DataAccessException ex) {
            return failed(flash, id, "recipe", ex);
        }
        flash.addFlashAttribute("success", "Đã tạo bản sao. Hãy sửa tên, giá và danh sách bánh.");
        return "redirect:/admin/gift-boxes/" + copyId + "#recipe";
    }

    private String failed(RedirectAttributes flash, long id, String target, Exception ex) {
        String message = "Không lưu được vì dữ liệu vừa được thay đổi. Hãy tải lại trang và thử lại.";
        if (ex instanceof MoonCakeException) {
            message = ex.getMessage();
        }
        flash.addFlashAttribute("formTarget", target);
        flash.addFlashAttribute("formError", message);
        return "redirect:/admin/gift-boxes/" + id + "#" + target;
    }
}
