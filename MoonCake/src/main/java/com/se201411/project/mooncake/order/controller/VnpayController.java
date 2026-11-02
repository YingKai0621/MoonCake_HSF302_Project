package com.se201411.project.mooncake.order.controller;
import com.se201411.project.mooncake.order.service.*;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
@Controller
public class VnpayController {
 private final VnpayPaymentService service; private final VnpayGateway gateway;
 public VnpayController(VnpayPaymentService service,VnpayGateway gateway){this.service=service;this.gateway=gateway;}
 @PostMapping("/orders/{code}/pay-vnpay") public String pay(@PathVariable String code,HttpServletRequest request,RedirectAttributes flash){
  try{return "redirect:"+service.start(code,request.getRemoteAddr());}
  catch(MoonCakeException e){flash.addFlashAttribute("errorMessage",e.getMessage());return "redirect:/orders/"+code;}
 }
 @GetMapping("/payments/vnpay/ipn") @ResponseBody public Map<String,String> ipn(@RequestParam Map<String,String> p,HttpServletRequest request){
  if(request.getParameterMap().values().stream().anyMatch(v->v.length!=1))return Map.of("RspCode","97","Message","Duplicate parameters");
  return service.notify(p);
 }
 @GetMapping("/payments/vnpay/return") public String result(@RequestParam Map<String,String> p,RedirectAttributes flash){
  boolean valid=gateway.valid(p);
  flash.addFlashAttribute("successMessage",valid?"Đã trở về từ VNPay. Trạng thái thanh toán được cập nhật sau xác nhận của VNPay; hãy tải lại đơn hàng.":"Không xác minh được kết quả VNPay.");
  return "redirect:/orders";
 }
}
