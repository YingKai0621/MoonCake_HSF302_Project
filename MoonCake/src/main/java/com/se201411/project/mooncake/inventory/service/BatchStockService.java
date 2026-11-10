package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.AdminBatchView;

import java.time.LocalDate;
import java.util.List;

public interface BatchStockService {

    void apply(long variantId, int delta, String operationKey, Long orderId, StockMovementType type);

    void finishPack(String operationGroup, long variantId);

    List<AdminBatchView> list();

    void updateTraceability(long batchId, String code, LocalDate producedOn, LocalDate expiresOn);

    void labelReceivedBatch(String operationKey, String code, LocalDate producedOn, LocalDate expiresOn);
}
