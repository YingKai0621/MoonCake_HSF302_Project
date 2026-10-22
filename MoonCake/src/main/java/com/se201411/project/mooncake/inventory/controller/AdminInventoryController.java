package com.se201411.project.mooncake.inventory.controller;

import com.se201411.project.mooncake.inventory.service.*;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String inventory(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("balances", inventory.balances());
        model.addAttribute("movements", inventory.movements(page));
        model.addAttribute("presets", presets.presets());
        model.addAttribute("page", Math.max(0, page));
        model.addAttribute("receiveKey", UUID.randomUUID().toString());
        model.addAttribute("adjustKey", UUID.randomUUID().toString());
        model.addAttribute("packKey", UUID.randomUUID().toString());
        return "admin/inventory";
    }

    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute AdminStockForm form, RedirectAttributes flash) {
        inventory.receive(form);
        return done(flash);
    }

    @PostMapping("/adjust")
    public String adjust(@Valid @ModelAttribute AdminStockForm form, RedirectAttributes flash) {
        inventory.adjust(form);
        return done(flash);
    }

    @PostMapping("/pack")
    public String pack(@Valid @ModelAttribute AdminStockForm form, RedirectAttributes flash) {
        packing.pack(form);
        return done(flash);
    }

    private String done(RedirectAttributes flash) {
        flash.addFlashAttribute("success", "Đã ghi nhận kho và lịch sử.");
        return "redirect:/admin/inventory";
    }
}
