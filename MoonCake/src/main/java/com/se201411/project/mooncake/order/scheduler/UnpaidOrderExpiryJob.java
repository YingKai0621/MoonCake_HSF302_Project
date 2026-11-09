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

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void cancelExpiredUnpaidOrders() {
        unpaidOrderExpiryService.cancelExpiredUnpaidOrders();
    }
}
