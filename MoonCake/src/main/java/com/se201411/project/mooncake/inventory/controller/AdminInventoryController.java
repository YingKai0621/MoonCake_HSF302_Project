package com.se201411.project.mooncake.inventory.controller;

import com.se201411.project.mooncake.inventory.service.*;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.web.FormErrors;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockFilter;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {
    private final InventoryAdminService inventory;
    private final PackingService packing;
    private final PresetConfigurationAdminService presets;

    public AdminInventoryController(InventoryAdminService inventory, PackingService packing, PresetConfigurationAdminService presets) {
        this.inventory = inventory;
        this.packing = packing;
        this.presets = presets;
    }

    @GetMapping
    public String inventory(@RequestParam(defaultValue = "ALL") StockFilter filter,
                            @RequestParam(required = false) Long pack,
                            @RequestParam(required = false) Long giftBox,
                            @RequestParam(required = false) Long receive,
                            @RequestParam(required = false) Long returnProduct, Model model) {
        model.addAttribute("balances", inventory.balances(filter));
        model.addAttribute("tabs", inventory.stockTabs(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("presets", presets.presets());
        model.addAttribute("allBalances", inventory.balances());
        model.addAttribute("receiveKey", UUID.randomUUID().toString());
        model.addAttribute("adjustKey", UUID.randomUUID().toString());
        model.addAttribute("packKey", UUID.randomUUID().toString());
        model.addAttribute("packVariant", pack);
        if (giftBox != null) {
            model.addAttribute("returnGiftBox", giftBox);
        }
        model.addAttribute("receiveVariant", receive);
        if (returnProduct != null) {
            model.addAttribute("returnProduct", returnProduct);
        }
        return "admin/inventory";
    }

    @GetMapping("/movements")
    public String movements(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("movements", inventory.movements(page));
        model.addAttribute("page", Math.max(0, page));
        return "admin/inventory-movements";
    }

    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute AdminStockForm form, BindingResult result,
                          @RequestParam(required = false) String batchCode,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate producedOn,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                          @RequestParam(required = false) Long returnProduct, RedirectAttributes flash) {
        String error = null;
        if (result.hasErrors()) {
            error = FormErrors.first(result);
        } else {
            try {
                inventory.receive(form, batchCode, producedOn, expiresOn);
            } catch (MoonCakeException ex) {
                error = ex.getMessage();
            }
        }
        if (error != null) {
            if (returnProduct != null) {
                flash.addFlashAttribute("returnProduct", returnProduct);
            }
            return reopen(flash, "receive-dialog", form, error);
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
            return reopen(flash, "adjust-dialog", form, FormErrors.first(result));
        }
        try {
            inventory.adjust(form);
        } catch (MoonCakeException ex) {
            return reopen(flash, "adjust-dialog", form, ex.getMessage());
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
            if (returnGiftBox != null) {
                flash.addFlashAttribute("returnGiftBox", returnGiftBox);
            }
            return reopen(flash, "pack-dialog", form, error);
        }
        if (returnGiftBox != null) {
            flash.addFlashAttribute("success", "Đã đóng " + form.quantity() + " hộp.");
            return "redirect:/admin/gift-boxes/" + returnGiftBox + "#sale";
        }
        return done(flash);
    }

    private String reopen(RedirectAttributes flash, String dialogId, AdminStockForm form, String message) {
        flash.addFlashAttribute("dialogId", dialogId);
        flash.addFlashAttribute("dialogError", message);
        flash.addFlashAttribute("dialogForm", form);
        return "redirect:/admin/inventory";
    }

    private String done(RedirectAttributes flash) {
        flash.addFlashAttribute("success", "Đã cập nhật tồn kho.");
        return "redirect:/admin/inventory";
    }
}
