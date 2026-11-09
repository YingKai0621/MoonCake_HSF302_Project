package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.order.service.OperationsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OperationsController {

    private final OperationsService operationsService;

    public OperationsController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping("/admin/overview")
    public String overview(Model model) {
        model.addAttribute("stats", operationsService.statistics());
        model.addAttribute("businesses", operationsService.businesses());
        return "admin/overview";
    }
}
