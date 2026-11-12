package com.se201411.project.mooncake.inventory.service;

import java.util.Map;
import java.util.Collection;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.AdminBatchView;

import java.time.LocalDate;
import java.util.List;

public interface BatchStockService {

    void apply(long variantId, int delta, String operationKey, Long orderId, StockMovementType type);

    void finishPack(String operationGroup, long variantId, LocalDate packedOn);

    List<AdminBatchView> list();

    void updateTraceability(long batchId, String code, LocalDate producedOn, LocalDate expiresOn);

    void updateCode(long batchId, String code);

    long variantIdOf(long batchId);

    void labelReceivedBatch(String operationKey, String code, LocalDate producedOn, LocalDate expiresOn);

    Map<Long, Integer> expiredQuantities(Collection<Long> variantIds);
}
