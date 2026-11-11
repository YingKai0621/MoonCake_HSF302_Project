package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.web.FormErrors;
import com.se201411.project.mooncake.product.dto.ProductVideoForm;
import com.se201411.project.mooncake.product.service.ProductMediaService;
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
public class ProductMediaController {

    private final ProductMediaService productMediaService;

    public ProductMediaController(ProductMediaService productMediaService) {
        this.productMediaService = productMediaService;
    }

    @GetMapping("/admin/media")
    public String page(Model model) {
        model.addAttribute("page", productMediaService.mediaPage());
        return "admin/media";
    }

    @PostMapping("/admin/media")
    public String add(
        @Valid @ModelAttribute ProductVideoForm form,
        BindingResult result,
        RedirectAttributes flash
    ) {
        if (result.hasErrors()) {
            return reopen(flash, form, FormErrors.first(result));
        }
        try {
            productMediaService.addVideo(form.productId(), form.url(), form.caption());
        } catch (MoonCakeException ex) {
            return reopen(flash, form, ex.getMessage());
        }
        flash.addFlashAttribute("success", "Đã thêm video.");
        return "redirect:/admin/media";
    }

    private String reopen(RedirectAttributes flash, ProductVideoForm form, String message) {
        flash.addFlashAttribute("dialogId", "video-new");
        flash.addFlashAttribute("dialogError", message);
        flash.addFlashAttribute("videoProductId", form.productId());
        flash.addFlashAttribute("videoUrl", form.url());
        flash.addFlashAttribute("videoCaption", form.caption());
        return "redirect:/admin/media";
    }

    @PostMapping("/admin/media/{id}/remove")
    public String remove(@PathVariable long id, RedirectAttributes flash) {
        productMediaService.removeVideo(id);
        flash.addFlashAttribute("success", "Đã xóa video.");
        return "redirect:/admin/media";
    }
}
