package com.se201411.project.mooncake.inventory.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.web.FormErrors;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockFilter;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.inventory.dto.BatchInfoForm;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.PackingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

    private final InventoryAdminService inventory;
    private final PackingService packing;

    public AdminInventoryController(InventoryAdminService inventory, PackingService packing) {
        this.inventory = inventory;
        this.packing = packing;
    }

    @GetMapping
    public String inventory(
        @RequestParam(defaultValue = "ALL") StockFilter filter,
        @RequestParam(required = false) Long pack,
        @RequestParam(required = false) Long giftBox,
        @RequestParam(required = false) Long receive,
        @RequestParam(required = false) Long returnProduct,
        Model model
    ) {
        model.addAttribute("page", inventory.inventoryPage(filter, receive, pack, returnProduct, giftBox));
        return "admin/inventory";
    }

    @GetMapping("/movements")
    public String movements(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", inventory.movementsPage(page));
        return "admin/inventory-movements";
    }

    @PostMapping("/receive")
    public String receive(
        @Valid @ModelAttribute AdminStockForm form,
        BindingResult result,
        @Valid @ModelAttribute BatchInfoForm batch,
        BindingResult batchResult,
        @RequestParam(required = false) Long returnProduct,
        RedirectAttributes flash
    ) {
        String error = null;
        if (result.hasErrors()) {
            error = FormErrors.first(result);
        } else if (batchResult.hasErrors()) {
            error = FormErrors.first(batchResult);
        } else {
            try {
                inventory.receive(form, batch.batchCode(), batch.producedOn());
            } catch (MoonCakeException ex) {
                error = ex.getMessage();
            }
        }
        if (error != null) {
            return reopen(flash, "receive-dialog", form, error, "?returnProduct=", returnProduct);
        }
        if (returnProduct != null) {
            flash.addFlashAttribute("success", "Đã nhập thêm " + form.quantity() + " vào kho.");
            return "redirect:/admin/catalog/products/" + returnProduct + "#sku-" + form.variantId();
        }
        return done(flash);
    }

    @PostMapping("/adjust")
    public String adjust(@Valid @ModelAttribute AdminStockForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return reopen(flash, "adjust-dialog", form, FormErrors.first(result), "", null);
        }
        try {
            inventory.adjust(form);
        } catch (MoonCakeException ex) {
            return reopen(flash, "adjust-dialog", form, ex.getMessage(), "", null);
        }
        return done(flash);
    }

    @PostMapping("/pack")
    public String pack(@Valid @ModelAttribute AdminStockForm form, BindingResult result,
                       @RequestParam(required = false) Long returnGiftBox, RedirectAttributes flash) {
        String error = null;
        if (result.hasErrors()) {
            error = FormErrors.first(result);
        } else {
            try {
                packing.pack(form);
            } catch (MoonCakeException ex) {
                error = ex.getMessage();
            }
        }
        if (error != null) {
            return reopen(flash, "pack-dialog", form, error, "?giftBox=", returnGiftBox);
        }
        if (returnGiftBox != null) {
            flash.addFlashAttribute("success", "Đã đóng " + form.quantity() + " hộp.");
            return "redirect:/admin/gift-boxes/" + returnGiftBox + "#sale";
        }
        return done(flash);
    }

    private String reopen(
        RedirectAttributes flash,
        String dialogId,
        AdminStockForm form,
        String message,
        String returnParameter,
        Long returnId
    ) {
        flash.addFlashAttribute("dialogId", dialogId);
        flash.addFlashAttribute("dialogError", message);
        flash.addFlashAttribute("dialogForm", form);
        if (returnId == null) {
            return "redirect:/admin/inventory";
        }
        return "redirect:/admin/inventory" + returnParameter + returnId;
    }

    private String done(RedirectAttributes flash) {
        flash.addFlashAttribute("success", "Đã cập nhật tồn kho.");
        return "redirect:/admin/inventory";
    }
}
