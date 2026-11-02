package com.se201411.project.mooncake.order.service;
import org.springframework.stereotype.Service;
import java.time.Clock;
@Service
public class VnpayReconciliationService {
 private final VnpayPaymentService payments; private final VnpayGateway gateway; private final Clock clock;
 public VnpayReconciliationService(VnpayPaymentService payments,VnpayGateway gateway,Clock clock){this.payments=payments;this.gateway=gateway;this.clock=clock;}
 public String check(String code,String ip){
  var attempt=payments.pendingOwned(code);
  if(attempt==null)return "Không có giao dịch đang chờ. Vui lòng xem trạng thái thanh toán bên dưới.";
  var result=gateway.query(attempt.ref(),attempt.createdAt(),ip,clock.instant());
  var applied=payments.applyQuery(result);
  if("00".equals(applied.get("RspCode")) || "02".equals(applied.get("RspCode")))
   return "Đã đối soát với VNPay. Vui lòng xem trạng thái thanh toán của đơn.";
  return "VNPay chưa xác nhận kết quả cuối cùng. Vui lòng kiểm tra lại sau hoặc liên hệ cửa hàng; không cần đặt đơn mới.";
 }
}
