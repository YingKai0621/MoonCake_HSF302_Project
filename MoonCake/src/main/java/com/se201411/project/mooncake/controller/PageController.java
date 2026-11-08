package com.se201411.project.mooncake.controller;

import com.se201411.project.mooncake.feedback.service.FeedbackService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final FeedbackService feedbackService;

    public PageController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/")
    public String homepage(Model model) {
        model.addAttribute("reviews", feedbackService.findPublishedReviews());
        return "homepage";
    }

    @GetMapping("/business")
    public String business() {
        return "business";
    }

}
