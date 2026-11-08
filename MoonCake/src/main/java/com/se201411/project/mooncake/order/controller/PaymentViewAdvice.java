package com.se201411.project.mooncake.order.controller;
import com.se201411.project.mooncake.order.service.VnpayGateway;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
@ControllerAdvice
public class PaymentViewAdvice {
 private final VnpayGateway gateway;
 public PaymentViewAdvice(VnpayGateway gateway){this.gateway=gateway;}
 @ModelAttribute("vnpayEnabled") public boolean enabled(){return gateway.enabled();}
}
