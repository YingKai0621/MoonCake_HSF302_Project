package com.se201411.project.mooncake.order.scheduler;

import com.se201411.project.mooncake.order.service.UnpaidOrderExpiryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UnpaidOrderExpiryJob {

    private final UnpaidOrderExpiryService unpaidOrderExpiryService;

    public UnpaidOrderExpiryJob(UnpaidOrderExpiryService unpaidOrderExpiryService) {
        this.unpaidOrderExpiryService = unpaidOrderExpiryService;
    }

    @Scheduled(
        initialDelayString = "${AUTO_CANCEL_CHECK_MS:300000}",
        fixedDelayString = "${AUTO_CANCEL_CHECK_MS:300000}"
    )
    public void cancelExpiredUnpaidOrders() {
        unpaidOrderExpiryService.cancelExpiredUnpaidOrders();
    }
}
