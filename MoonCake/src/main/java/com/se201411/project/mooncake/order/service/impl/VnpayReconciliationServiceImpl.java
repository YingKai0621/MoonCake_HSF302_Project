package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.order.dto.VnpayPendingAttempt;
import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import com.se201411.project.mooncake.order.service.VnpayReconciliationService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

/**
 * Not transactional on purpose: the HTTP call to VNPay must not hold a
 * database lock. Each database step runs in its own VnpayPaymentService
 * transaction.
 */
@Service
public class VnpayReconciliationServiceImpl implements VnpayReconciliationService {

    private final VnpayPaymentService vnpayPaymentService;
    private final VnpayGateway gateway;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public VnpayReconciliationServiceImpl(
        VnpayPaymentService vnpayPaymentService,
        VnpayGateway gateway,
        AdminAccessService adminAccessService,
        Clock clock
    ) {
        this.vnpayPaymentService = vnpayPaymentService;
        this.gateway = gateway;
        this.adminAccessService = adminAccessService;
        this.clock = clock;
    }

    @Override
    public String checkPayment(String orderCode, String clientIp) {
        // Ownership is checked inside findPendingAttempt (current customer only).
        VnpayPendingAttempt attempt = vnpayPaymentService.findPendingAttempt(orderCode);
        return reconcile(attempt, clientIp);
    }

    @Override
    public String checkPaymentForAdmin(long orderId, String clientIp) {
        adminAccessService.requireAdmin();
        VnpayPendingAttempt attempt = vnpayPaymentService.findPendingAttemptForOrder(orderId);
        return reconcile(attempt, clientIp);
    }

    private String reconcile(VnpayPendingAttempt attempt, String clientIp) {
        if (attempt == null) {
            return "Không có giao dịch VNPay đang chờ. Vui lòng xem trạng thái thanh toán của đơn.";
        }

        Map<String, String> queryResult = gateway.query(
            attempt.transactionRef(),
            attempt.createdAt(),
            clientIp,
            clock.instant()
        );
        Map<String, String> applied = vnpayPaymentService.applyQueryResult(queryResult);

        String responseCode = applied.get("RspCode");
        if ("00".equals(responseCode) || "02".equals(responseCode)) {
            return "Đã đối soát với VNPay. Vui lòng xem trạng thái thanh toán của đơn.";
        }
        return "VNPay chưa xác nhận kết quả cuối cùng (giao dịch chỉ được đóng sau khi hết hạn khoảng 20 phút). "
            + "Vui lòng kiểm tra lại sau; không cần đặt đơn mới.";
    }
}
