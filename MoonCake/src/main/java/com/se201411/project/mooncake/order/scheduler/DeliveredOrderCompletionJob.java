package com.se201411.project.mooncake.order.scheduler;

import com.se201411.project.mooncake.order.service.DeliveredOrderCompletionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveredOrderCompletionJob {

    private final DeliveredOrderCompletionService completionService;

    public DeliveredOrderCompletionJob(
        DeliveredOrderCompletionService completionService
    ) {
        this.completionService = completionService;
    }

    @Scheduled(
        initialDelayString = "${AUTO_COMPLETE_CHECK_MS:3600000}",
        fixedDelayString = "${AUTO_COMPLETE_CHECK_MS:3600000}"
    )
    public void completeOverdueDeliveries() {
        completionService.completeOverdueDeliveries();
    }
}
