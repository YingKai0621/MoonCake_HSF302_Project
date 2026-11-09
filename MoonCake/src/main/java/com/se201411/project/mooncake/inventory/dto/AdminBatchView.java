package com.se201411.project.mooncake.inventory.dto;

import java.time.LocalDate;

public record AdminBatchView(
    long id,
    String sku,
    String batchCode,
    LocalDate producedOn,
    LocalDate expiresOn,
    int remainingQuantity,
    String statusLabel
) {
}
