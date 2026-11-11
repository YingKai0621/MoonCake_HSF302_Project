package com.se201411.project.mooncake.dashboard.controller;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/overview")
    public String overview(
        @RequestParam(defaultValue = "DAY") DashboardPeriod period,
        Model model
    ) {
        model.addAttribute("dashboard", dashboardService.dashboard(period));
        return "admin/overview";
    }
}
