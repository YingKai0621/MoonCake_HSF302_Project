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
                            @RequestParam(required = false) Long pack, Model model) {
        model.addAttribute("balances", inventory.balances(filter));
        model.addAttribute("tabs", inventory.stockTabs(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("presets", presets.presets());
        model.addAttribute("allBalances", inventory.balances());
        model.addAttribute("receiveKey", UUID.randomUUID().toString());
        model.addAttribute("adjustKey", UUID.randomUUID().toString());
        model.addAttribute("packKey", UUID.randomUUID().toString());
        model.addAttribute("packVariant", pack);
        return "admin/inventory";
    }

    @GetMapping("/movements")
    public String movements(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("movements", inventory.movements(page));
        model.addAttribute("page", Math.max(0, page));
        return "admin/inventory-movements";
    }

    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute AdminStockForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return reopen(flash, "receive-dialog", form, FormErrors.first(result));
        }
        try {
            inventory.receive(form);
        } catch (MoonCakeException ex) {
            return reopen(flash, "receive-dialog", form, ex.getMessage());
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
    public String pack(@Valid @ModelAttribute AdminStockForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return reopen(flash, "pack-dialog", form, FormErrors.first(result));
        }
        try {
            packing.pack(form);
        } catch (MoonCakeException ex) {
            return reopen(flash, "pack-dialog", form, ex.getMessage());
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
