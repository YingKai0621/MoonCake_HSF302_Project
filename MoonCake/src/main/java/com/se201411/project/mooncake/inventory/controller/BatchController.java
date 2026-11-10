package com.se201411.project.mooncake.inventory.controller;

import com.se201411.project.mooncake.inventory.service.BatchStockService;
import org.springframework.format.annotation.DateTimeFormat;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class BatchController {

    private final BatchStockService batchStockService;

    public BatchController(BatchStockService batchStockService) {
        this.batchStockService = batchStockService;
    }

    @GetMapping("/admin/batches")
    public String page(Model model) {
        model.addAttribute("batches", batchStockService.list());
        return "admin/batches";
    }

    @PostMapping("/admin/batches/{id}")
    public String save(
        @PathVariable long id,
        @RequestParam String code,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate production,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiry,
        RedirectAttributes flash
    ) {
        try {
            batchStockService.updateTraceability(id, code, production, expiry);
        } catch (MoonCakeException ex) {
            flash.addFlashAttribute("dialogId", "batch-" + id);
            flash.addFlashAttribute("dialogError", ex.getMessage());
            return "redirect:/admin/batches";
        }
        flash.addFlashAttribute("success", "Đã cập nhật thông tin lô.");
        return "redirect:/admin/batches";
    }
}
