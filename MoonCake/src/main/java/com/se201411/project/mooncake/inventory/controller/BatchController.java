package com.se201411.project.mooncake.inventory.controller;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
@Controller
public class BatchController {
 private final BatchStockService service;
 public BatchController(BatchStockService service){this.service=service;}
 @GetMapping("/admin/batches") public String page(Model model){model.addAttribute("batches",service.list());return "admin/batches";}
 @PostMapping("/admin/batches/{id}") public String save(@PathVariable long id,@RequestParam String code,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate production,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate expiry,RedirectAttributes flash){service.dates(id,code,production,expiry);flash.addFlashAttribute("success","Đã cập nhật thông tin lô.");return "redirect:/admin/batches";}
}
