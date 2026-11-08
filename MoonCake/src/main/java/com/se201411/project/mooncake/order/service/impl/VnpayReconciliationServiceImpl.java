package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.order.dto.VnpayPendingAttempt;
import com.se201411.project.mooncake.order.service.VnpayPaymentService;
import com.se201411.project.mooncake.order.service.VnpayReconciliationService;
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
    private final Clock clock;

    public VnpayReconciliationServiceImpl(
        VnpayPaymentService vnpayPaymentService,
        VnpayGateway gateway,
        Clock clock
    ) {
        this.vnpayPaymentService = vnpayPaymentService;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public String checkPayment(String orderCode, String clientIp) {
        VnpayPendingAttempt attempt = vnpayPaymentService.findPendingAttempt(orderCode);
        if (attempt == null) {
            return "Không có giao dịch đang chờ. Vui lòng xem trạng thái thanh toán bên dưới.";
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
        return "VNPay chưa xác nhận kết quả cuối cùng. Vui lòng kiểm tra lại sau hoặc liên hệ cửa hàng; không cần đặt đơn mới.";
    }
}
