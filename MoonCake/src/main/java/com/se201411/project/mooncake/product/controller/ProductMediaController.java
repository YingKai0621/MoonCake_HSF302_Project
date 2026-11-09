package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.product.service.ProductMediaService;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductMediaController {

    private final ProductMediaService productMediaService;
    private final ProductAdminService productAdminService;

    public ProductMediaController(
        ProductMediaService productMediaService,
        ProductAdminService productAdminService
    ) {
        this.productMediaService = productMediaService;
        this.productAdminService = productAdminService;
    }

    @GetMapping("/admin/media")
    public String page(Model model) {
        model.addAttribute("videos", productMediaService.allVideos());
        model.addAttribute("catalog", productAdminService.catalog());
        return "admin/media";
    }

    @PostMapping("/admin/media")
    public String add(
        @RequestParam long productId,
        @RequestParam String url,
        @RequestParam String caption,
        RedirectAttributes flash
    ) {
        try {
            productMediaService.addVideo(productId, url, caption);
        } catch (MoonCakeException ex) {
            flash.addFlashAttribute("dialogId", "video-new");
            flash.addFlashAttribute("dialogError", ex.getMessage());
            flash.addFlashAttribute("videoProductId", productId);
            flash.addFlashAttribute("videoUrl", url);
            flash.addFlashAttribute("videoCaption", caption);
            return "redirect:/admin/media";
        }
        flash.addFlashAttribute("success", "Đã thêm video.");
        return "redirect:/admin/media";
    }

    @PostMapping("/admin/media/{id}/remove")
    public String remove(@PathVariable long id, RedirectAttributes flash) {
        productMediaService.removeVideo(id);
        flash.addFlashAttribute("success", "Đã xóa video.");
        return "redirect:/admin/media";
    }
}
