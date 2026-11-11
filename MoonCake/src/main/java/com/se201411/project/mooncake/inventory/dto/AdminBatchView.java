package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.StockBatch;
import java.time.LocalDate;

public record AdminBatchView(
    long id,
    String sku,
    String productName,
    String batchCode,
    LocalDate producedOn,
    LocalDate expiresOn,
    int remainingQuantity,
    String statusLabel,
    String statusTone
) {

    public static AdminBatchView entityToDto(StockBatch batch, String batchCodeLabel, String statusLabel, String statusTone) {
        ProductVariant variant = batch.getVariant();
        return new AdminBatchView(
            batch.getId(),
            variant.getSku(),
            variant.getProduct().getName() + " · " + variant.getName(),
            batchCodeLabel,
            batch.getProducedOn(),
            batch.getExpiresOn(),
            batch.getRemainingQuantity(),
            statusLabel,
            statusTone
        );
    }
}
