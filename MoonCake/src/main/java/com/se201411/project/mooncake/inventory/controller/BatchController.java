package com.se201411.project.mooncake.inventory.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.web.FormErrors;
import com.se201411.project.mooncake.inventory.dto.BatchInfoForm;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        @Valid @ModelAttribute BatchInfoForm form,
        BindingResult result,
        RedirectAttributes flash
    ) {
        if (result.hasErrors()) {
            return reopen(flash, id, FormErrors.first(result));
        }
        try {
            batchStockService.updateTraceability(id, form.batchCode(), form.producedOn(), form.expiresOn());
        } catch (MoonCakeException ex) {
            return reopen(flash, id, ex.getMessage());
        }
        flash.addFlashAttribute("success", "Đã cập nhật thông tin lô.");
        return "redirect:/admin/batches";
    }

    private String reopen(RedirectAttributes flash, long id, String message) {
        flash.addFlashAttribute("dialogId", "batch-" + id);
        flash.addFlashAttribute("dialogError", message);
        return "redirect:/admin/batches";
    }
}
