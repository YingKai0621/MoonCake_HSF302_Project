package com.se201411.project.mooncake.order.controller;
import com.se201411.project.mooncake.order.service.OperationsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class OperationsController {
 private final OperationsService service;
 public OperationsController(OperationsService service){this.service=service;}
 @GetMapping("/admin/overview") public String overview(Model model){model.addAttribute("stats",service.statistics());model.addAttribute("businesses",service.businesses());return "admin/overview";}
}
