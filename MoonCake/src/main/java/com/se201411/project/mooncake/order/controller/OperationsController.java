package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.service.DashboardService;
import com.se201411.project.mooncake.order.service.OperationsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OperationsController {

    private final OperationsService operationsService;
    private final DashboardService dashboardService;

    public OperationsController(OperationsService operationsService, DashboardService dashboardService) {
        this.operationsService = operationsService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/overview")
    public String overview(
        @RequestParam(defaultValue = "DAY") DashboardPeriod period,
        Model model
    ) {
        model.addAttribute("dashboard", dashboardService.dashboard(period));
        model.addAttribute("periods", DashboardPeriod.values());
        model.addAttribute("businesses", operationsService.businesses());
        return "admin/overview";
    }
}
