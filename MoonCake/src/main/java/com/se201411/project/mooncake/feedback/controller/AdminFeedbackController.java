package com.se201411.project.mooncake.feedback.controller;

import com.se201411.project.mooncake.feedback.service.FeedbackService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("feedbacks", feedbackService.findForAdmin());
        return "admin/feedback";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable long id, RedirectAttributes flash) {
        feedbackService.publish(id);
        flash.addFlashAttribute("success", "Đã hiển thị đánh giá trên trang chủ.");
        return "redirect:/admin/feedback";
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable long id, RedirectAttributes flash) {
        feedbackService.hide(id);
        flash.addFlashAttribute("success", "Đã ẩn đánh giá khỏi trang chủ.");
        return "redirect:/admin/feedback";
    }
}
