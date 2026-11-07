package com.se201411.project.mooncake.order.controller;
import com.se201411.project.mooncake.order.service.OperationsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class OperationsController {
 private final OperationsService service;
 public OperationsController(OperationsService service){this.service=service;}
 @GetMapping("/feedback") public String feedback(){return "feedback";}
 @PostMapping("/feedback") public String send(@RequestParam String content,RedirectAttributes flash){try { service.feedback(content); } catch(com.se201411.project.mooncake.common.exception.BusinessRuleViolationException e) { flash.addFlashAttribute("errorMessage",e.getMessage());flash.addFlashAttribute("content",content);return "redirect:/feedback"; } flash.addFlashAttribute("successMessage","Cảm ơn bạn đã góp ý!");return "redirect:/feedback";}
 @GetMapping("/admin/overview") public String overview(Model model){model.addAttribute("stats",service.statistics());model.addAttribute("businesses",service.businesses());model.addAttribute("feedbacks",service.feedbacks());return "admin/overview";}
 @PostMapping("/orders/{code}/address") public String address(@PathVariable String code,@RequestParam String address,RedirectAttributes flash){try { service.address(code,address); } catch(com.se201411.project.mooncake.common.exception.BusinessRuleViolationException e) { flash.addFlashAttribute("errorMessage",e.getMessage());return "redirect:/orders/"+code; } flash.addFlashAttribute("successMessage","Đã cập nhật địa chỉ giao hàng.");return "redirect:/orders/"+code;}
}
