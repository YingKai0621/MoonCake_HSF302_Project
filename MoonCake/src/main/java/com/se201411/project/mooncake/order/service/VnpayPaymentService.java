package com.se201411.project.mooncake.order.service;
import com.se201411.project.mooncake.order.repository.*;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
@Service
public class VnpayPaymentService {
 private final VnpayRepository repo; private final PaymentRepository payments;
 private final CurrentCustomerService customers; private final VnpayGateway gateway; private final Clock clock;
 public VnpayPaymentService(VnpayRepository repo,PaymentRepository payments,CurrentCustomerService customers,VnpayGateway gateway,Clock clock){
  this.repo=repo;this.payments=payments;this.customers=customers;this.gateway=gateway;this.clock=clock;
 }
 @Transactional public String start(String code,String ip){
  gateway.requireConfigured();
  var order=repo.lockOwned(code,customers.requireCurrentCustomer().customerId());
  var list=payments.findByOrderId(order.getId());
  if(order.getStatus()!=OrderStatus.PLACED || list.size()!=1 || list.get(0).getStatus()!=PaymentStatus.UNPAID || order.getTotalAmount().signum()<=0)
   throw new BusinessRuleViolationException("Chỉ thanh toán online cho đơn chờ xử lý, chưa trả tiền.");
  var pending=repo.pendingAttempt(order.getId());
  if(pending!=null){
   Instant created=pending.createdAt().toInstant(ZoneOffset.UTC);
   if(clock.instant().isBefore(created.plusSeconds(900)))return gateway.url(pending.ref(),pending.amount(),ip,created);
   throw new BusinessRuleViolationException("Phiên VNPay đã hết thời gian. Bấm Kiểm tra thanh toán để đối soát trước khi thanh toán lại.");
  }
  var payment=list.get(0); payment.setMethod(PaymentMethod.VNPAY); payment.setUpdatedAt(LocalDateTime.now(clock));payments.save(payment);
  String ref=UUID.randomUUID().toString().replace("-","");
  repo.create(ref,order.getId(),payment.getAmount(),LocalDateTime.now(clock));
  return gateway.url(ref,payment.getAmount(),ip,clock.instant());
 }
 @Transactional public Map<String,String> notify(Map<String,String> p){
  if(!gateway.valid(p))return response("97","Invalid signature");
  return applyVerified(p);
 }
 private Map<String,String> applyVerified(Map<String,String> p){
  if(p.get("vnp_TxnRef")==null)return response("01","Order not found");
  var first=repo.attempt(p.get("vnp_TxnRef")); if(first==null)return response("01","Order not found");
  var order=repo.lock(((Number)first[0]).longValue());
  var attempt=repo.attempt(p.get("vnp_TxnRef"));
  BigDecimal received;
  try{received=new BigDecimal(p.get("vnp_Amount")).movePointLeft(2);}catch(RuntimeException ex){return response("04","Invalid amount");}
  var list=payments.findByOrderId(order.getId());
  if(list.size()!=1 || received.compareTo((BigDecimal)attempt[1])!=0 || received.compareTo(order.getTotalAmount())!=0)return response("04","Invalid amount");
  var payment=list.get(0);
  if(!"PENDING".equals(attempt[2]) || payment.getStatus()==PaymentStatus.PAID)return response("02","Already confirmed");
  if(order.getStatus()==OrderStatus.CANCELLED || payment.getMethod()!=PaymentMethod.VNPAY)return response("01","Invalid order state");
  boolean success="00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
  String txn=p.get("vnp_TransactionNo");
  if(success && (txn==null || !txn.matches("[0-9]{1,50}")))return response("99","Invalid transaction");
  // Pending, reversal and refund states are not safe evidence of a failed charge.
  if(!success && !"02".equals(p.get("vnp_TransactionStatus")))return response("99","Awaiting final transaction status");
  repo.finish(p.get("vnp_TxnRef"),success?"PAID":"FAILED",success?txn:null);
  if(success){payment.setStatus(PaymentStatus.PAID);payment.setPaidAt(LocalDateTime.now(clock));payment.setUpdatedAt(LocalDateTime.now(clock));payments.save(payment);}
  return response("00","Confirm Success");
 }
 @Transactional public VnpayRepository.Pending pendingOwned(String code){
  var order=repo.lockOwned(code,customers.requireCurrentCustomer().customerId());
  return repo.pendingAttempt(order.getId());
 }
 @Transactional public Map<String,String> applyQuery(Map<String,String> p){
  if(!gateway.validQuery(p,p.getOrDefault("vnp_TxnRef","")))return response("97","Invalid signature");
  if(!"00".equals(p.get("vnp_ResponseCode")) || !"01".equals(p.get("vnp_TransactionType")))return response("99","Awaiting final transaction status");
  return applyVerified(p);
 }
 private Map<String,String> response(String code,String message){return Map.of("RspCode",code,"Message",message);}
}
