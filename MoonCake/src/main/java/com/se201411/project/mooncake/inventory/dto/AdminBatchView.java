package com.se201411.project.mooncake.inventory.dto;

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
}
