package com.se201411.project.mooncake.product.controller;
import com.se201411.project.mooncake.product.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class ProductMediaController {
 private final ProductMediaService media;private final ProductAdminService catalog;
 public ProductMediaController(ProductMediaService media,ProductAdminService catalog){this.media=media;this.catalog=catalog;}
 @GetMapping("/admin/media") public String page(Model model){model.addAttribute("videos",media.all());model.addAttribute("catalog",catalog.catalog());return "admin/media";}
 @PostMapping("/admin/media") public String add(@RequestParam long productId,@RequestParam String url,@RequestParam String caption,RedirectAttributes flash){media.add(productId,url,caption);flash.addFlashAttribute("success","Đã thêm video.");return "redirect:/admin/media";}
 @PostMapping("/admin/media/{id}/remove") public String remove(@PathVariable long id){media.remove(id);return "redirect:/admin/media";}
}
