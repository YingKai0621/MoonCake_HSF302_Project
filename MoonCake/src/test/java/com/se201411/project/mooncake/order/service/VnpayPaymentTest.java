package com.se201411.project.mooncake.order.service;
import com.se201411.project.mooncake.order.repository.*;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class VnpayPaymentTest {
 final VnpayRepository repo=mock(VnpayRepository.class);
 final PaymentRepository payments=mock(PaymentRepository.class);
 final VnpayGateway gateway=new VnpayGateway(new MockEnvironment().withProperty("VNPAY_ENABLED","true")
  .withProperty("VNPAY_TMN_CODE","TESTCODE").withProperty("VNPAY_HASH_SECRET","test-only-secret"));
 final VnpayPaymentService service=new VnpayPaymentService(repo,payments,mock(CurrentCustomerService.class),gateway,Clock.systemUTC());
 Map<String,String> callback(){
  var p=new HashMap<String,String>();p.put("vnp_TmnCode","TESTCODE");p.put("vnp_TxnRef","ref");p.put("vnp_Amount","10000000");
  p.put("vnp_ResponseCode","00");p.put("vnp_TransactionStatus","00");p.put("vnp_TransactionNo","12345");
  p.put("vnp_SecureHash",gateway.signature(p));return p;
 }
 void setup(){
  var order=mock(SalesOrder.class);when(order.getId()).thenReturn(1L);when(order.getTotalAmount()).thenReturn(new BigDecimal("100000"));when(order.getStatus()).thenReturn(OrderStatus.PLACED);
  when(repo.attempt("ref")).thenReturn(new Object[]{1L,new BigDecimal("100000"),"PENDING"});when(repo.lock(1L)).thenReturn(order);
  var payment=new Payment();payment.setMethod(PaymentMethod.VNPAY);payment.setAmount(new BigDecimal("100000"));when(payments.findByOrderId(1L)).thenReturn(List.of(payment));
 }
 @Test void publicUrlBuildsReturnUrl(){var g=new VnpayGateway(new MockEnvironment().withProperty("APP_PUBLIC_URL","https://demo.example.test/"));assertThat(g.returnUrl()).isEqualTo("https://demo.example.test/payments/vnpay/return");}
 @Test void enabledWithoutCredentialsFailsAtStartup(){var g=new VnpayGateway(new MockEnvironment().withProperty("VNPAY_ENABLED","true"));assertThatThrownBy(g::validateStartup).isInstanceOf(com.se201411.project.mooncake.common.exception.BusinessRuleViolationException.class);}
 @Test void tamperedSignatureCannotWrite(){var p=callback();p.put("vnp_Amount","1");assertThat(service.notify(p).get("RspCode")).isEqualTo("97");verifyNoInteractions(repo,payments);}
 @Test void wrongAmountCannotMarkPaid(){setup();var p=callback();p.put("vnp_Amount","999");p.put("vnp_SecureHash",gateway.signature(p));assertThat(service.notify(p).get("RspCode")).isEqualTo("04");verify(payments,never()).save(any());}
 @Test void validIpnMarksPaid(){setup();assertThat(service.notify(callback()).get("RspCode")).isEqualTo("00");assertThat(payments.findByOrderId(1L).get(0).getStatus()).isEqualTo(PaymentStatus.PAID);verify(repo).finish("ref","PAID","12345");}
 @Test void replayDoesNotPayTwice(){setup();service.notify(callback());assertThat(service.notify(callback()).get("RspCode")).isEqualTo("02");verify(payments,times(1)).save(any());}
 @Test void cancelledOrderCannotBePaid(){setup();when(repo.lock(1L).getStatus()).thenReturn(OrderStatus.CANCELLED);assertThat(service.notify(callback()).get("RspCode")).isEqualTo("01");verify(payments,never()).save(any());}
 @Test void failureDoesNotPay(){setup();var p=callback();p.put("vnp_ResponseCode","24");p.put("vnp_TransactionStatus","02");p.put("vnp_SecureHash",gateway.signature(p));assertThat(service.notify(p).get("RspCode")).isEqualTo("00");verify(repo).finish("ref","FAILED",null);verify(payments,never()).save(any());}
 @Test void buildsSandboxUrlWithAmountInHundredths(){String url=gateway.url("ref",new BigDecimal("100000"),"127.0.0.1",Instant.parse("2026-11-02T00:00:00Z"));assertThat(url).contains("vnp_Amount=10000000","vnp_Version=2.1.0","vnp_CreateDate=20261102070000","vnp_ExpireDate=20261102071500","vnp_SecureHash=");}
 @Test void pendingOrReversalCannotReleasePaymentForRetry(){
  setup();
  for(String state:List.of("01","04","05","06","07","09")){
   var p=callback();p.put("vnp_ResponseCode","99");p.put("vnp_TransactionStatus",state);p.put("vnp_SecureHash",gateway.signature(p));
   assertThat(service.notify(p).get("RspCode")).isEqualTo("99");
  }
  verify(repo,never()).finish(anyString(),anyString(),any());verify(payments,never()).save(any());
 }
 @Test void disabledGatewayRejectsPublicCallbackWithoutException(){
  assertThat(new VnpayGateway(new MockEnvironment()).valid(callback())).isFalse();
 }
 @Test void querySignatureProtectsAmountReferenceAndTransactionType(){
  var p=new HashMap<String,String>();
  p.put("vnp_ResponseId","response");p.put("vnp_Command","querydr");p.put("vnp_ResponseCode","00");p.put("vnp_Message","Success");
  p.put("vnp_TmnCode","TESTCODE");p.put("vnp_TxnRef","ref");p.put("vnp_Amount","10000000");p.put("vnp_BankCode","NCB");
  p.put("vnp_TransactionNo","12345");p.put("vnp_TransactionType","01");p.put("vnp_TransactionStatus","00");p.put("vnp_OrderInfo","test");
  p.put("vnp_SecureHash",gateway.sign("response|querydr|00|Success|TESTCODE|ref|10000000|NCB||12345|01|00|test||"));
  assertThat(gateway.validQuery(p,"ref")).isTrue();
  assertThat(gateway.validQuery(p,"other")).isFalse();
  setup();assertThat(service.applyQuery(p).get("RspCode")).isEqualTo("00");verify(repo).finish("ref","PAID","12345");
  p.put("vnp_Amount","1");assertThat(gateway.validQuery(p,"ref")).isFalse();
 }
}
