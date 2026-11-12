package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.common.format.VietnamTime;
import com.se201411.project.mooncake.entity.StockBatch;

import java.time.LocalDate;

public record SellableBatchView(
    long variantId,
    LocalDate producedOn,
    LocalDate expiresOn
) {
    public static SellableBatchView entityToDto(StockBatch batch) {
        return new SellableBatchView(batch.getVariantId(), batch.getProducedOn(), batch.getExpiresOn());
    }

    public String label() {
        if (expiresOn == null) {
            return null;
        }
        if (producedOn == null) {
            return "HSD " + VietnamTime.date(expiresOn);
        }
        return "NSX " + VietnamTime.date(producedOn) + " · HSD " + VietnamTime.date(expiresOn);
    }
}
