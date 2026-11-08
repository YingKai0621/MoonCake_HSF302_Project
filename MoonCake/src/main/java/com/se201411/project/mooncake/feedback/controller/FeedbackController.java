package com.se201411.project.mooncake.feedback.controller;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.feedback.dto.FeedbackForm;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/feedback")
    public String showForm(Model model) {
        if (!model.containsAttribute("feedbackForm")) {
            model.addAttribute("feedbackForm", new FeedbackForm());
        }
        return "feedback";
    }

    @PostMapping("/feedback")
    public String submit(
        @Valid @ModelAttribute("feedbackForm") FeedbackForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes flash
    ) {
        if (bindingResult.hasErrors()) {
            return "feedback";
        }
        try {
            feedbackService.submit(form);
        } catch (BusinessRuleViolationException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "feedback";
        }
        flash.addFlashAttribute(
            "successMessage",
            "Cảm ơn bạn đã góp ý! Đánh giá sẽ hiển thị trên trang chủ sau khi cửa hàng duyệt."
        );
        return "redirect:/feedback";
    }
}
