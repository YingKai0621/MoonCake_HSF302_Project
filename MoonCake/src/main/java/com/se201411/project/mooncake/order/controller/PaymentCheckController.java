package com.se201411.project.mooncake.order.controller;
import com.se201411.project.mooncake.order.service.VnpayReconciliationService;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class PaymentCheckController {
 private final VnpayReconciliationService service;
 public PaymentCheckController(VnpayReconciliationService service){this.service=service;}
 @PostMapping("/orders/{code}/payment-check")
 public String check(@PathVariable String code,HttpServletRequest request,RedirectAttributes flash){
  try{flash.addFlashAttribute("successMessage",service.check(code,request.getRemoteAddr()));}
  catch(MoonCakeException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
  return "redirect:/orders/"+code;
 }
}
