package com.se201411.project.mooncake.inventory.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.BatchAllocation;
import com.se201411.project.mooncake.entity.StockBatch;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.AdminBatchView;
import com.se201411.project.mooncake.inventory.dto.BatchQuantity;
import com.se201411.project.mooncake.inventory.repository.BatchAllocationRepository;
import com.se201411.project.mooncake.inventory.repository.InventoryBalanceRepository;
import com.se201411.project.mooncake.inventory.repository.StockBatchRepository;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchStockServiceImpl implements BatchStockService {

    private static final int MAX_CODE_LENGTH = 80;

    private final StockBatchRepository stockBatchRepository;
    private final BatchAllocationRepository batchAllocationRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public BatchStockServiceImpl(
        StockBatchRepository stockBatchRepository,
        BatchAllocationRepository batchAllocationRepository,
        InventoryBalanceRepository inventoryBalanceRepository,
        AdminAccessService adminAccessService,
        Clock clock
    ) {
        this.stockBatchRepository = stockBatchRepository;
        this.batchAllocationRepository = batchAllocationRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.adminAccessService = adminAccessService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void apply(long variantId, int delta, String operationKey, Long orderId, StockMovementType type) {
        Integer physical = inventoryBalanceRepository.available(variantId);
        if (physical == null) {
            throw new ResourceNotFoundException("Không tìm thấy tồn kho của SKU #" + variantId);
        }
        int before = Math.subtractExact(physical, delta);
        int tracked = Math.toIntExact(stockBatchRepository.sumRemaining(variantId));
        if (before > tracked) {
            createBatch(variantId, "LEGACY-" + shortId(), before - tracked);
        }
        if (before < tracked) {
            throw new BusinessRuleViolationException(
                "Tồn lô không khớp tồn tổng. Cần kiểm kê SKU #" + variantId
            );
        }

        if (delta > 0) {
            addStock(variantId, delta, operationKey, orderId, type);
        } else {
            takeStock(variantId, Math.negateExact(delta), operationKey, orderId, type);
        }
    }

    @Override
    @Transactional
    public void finishPack(String operationGroup, long variantId) {
        LocalDate earliestExpiry = batchAllocationRepository.findEarliestExpiryConsumed(
            operationGroup + ":",
            ProductType.MOONCAKE
        );
        String outputKey = operationGroup + ":" + variantId + ":IN";
        for (StockBatch batch : batchAllocationRepository.findBatchesCreatedBy(outputKey)) {
            batch.setExpiresOn(earliestExpiry);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBatchView> list() {
        adminAccessService.requireAdmin();
        LocalDate today = LocalDate.now(clock);
        List<AdminBatchView> views = new ArrayList<>();
        for (StockBatch batch : stockBatchRepository.findAllByOrderByIdDesc()) {
            views.add(new AdminBatchView(
                batch.getId(),
                batch.getVariant().getSku(),
                batch.getBatchCode(),
                batch.getProducedOn(),
                batch.getExpiresOn(),
                batch.getRemainingQuantity(),
                statusLabel(batch.getExpiresOn(), today)
            ));
        }
        return views;
    }

    @Override
    @Transactional
    public void updateTraceability(long batchId, String code, LocalDate producedOn, LocalDate expiresOn) {
        adminAccessService.requireAdmin();
        boolean invalidCode = code == null || code.isBlank() || code.length() > MAX_CODE_LENGTH;
        boolean invalidDates = producedOn != null && expiresOn != null && expiresOn.isBefore(producedOn);
        if (invalidCode || invalidDates) {
            throw new BusinessRuleViolationException(
                "Mã lô tối đa 80 ký tự. Hạn dùng phải sau ngày sản xuất."
            );
        }
        int changed = stockBatchRepository.updateTraceability(batchId, code.trim(), producedOn, expiresOn);
        if (changed != 1) {
            throw new BusinessRuleViolationException("Lô đã xuất hàng không thể đổi thông tin truy xuất.");
        }
    }

    private void addStock(long variantId, int quantity, String operationKey, Long orderId, StockMovementType type) {
        List<BatchQuantity> originalBatches = List.of();
        if (type == StockMovementType.CANCEL_RETURN && orderId != null) {
            originalBatches = batchAllocationRepository.findSoldForOrder(variantId, orderId);
        }

        if (originalBatches.isEmpty()) {
            StockBatch batch = createBatch(variantId, "LOT-" + shortId(), quantity);
            recordAllocation(batch.getId(), orderId, operationKey, quantity);
            return;
        }

        int restored = 0;
        for (BatchQuantity original : originalBatches) {
            int originalQuantity = Math.toIntExact(original.quantity());
            restored = Math.addExact(restored, originalQuantity);
            changeRemaining(original.batchId(), originalQuantity);
            recordAllocation(original.batchId(), orderId, operationKey, originalQuantity);
        }
        if (restored != quantity) {
            throw new BusinessRuleViolationException("Lượng hoàn lô không khớp đơn.");
        }
    }

    private void takeStock(long variantId, int needed, String operationKey, Long orderId, StockMovementType type) {
        List<StockBatch> batches;
        if (type == StockMovementType.ADJUST) {
            batches = stockBatchRepository.findAllWithStockForUpdate(variantId);
        } else {
            batches = stockBatchRepository.findSellableForUpdate(variantId, LocalDate.now(clock));
        }

        long usable = 0;
        for (StockBatch batch : batches) {
            usable += batch.getRemainingQuantity();
        }
        if (usable < needed) {
            throw new BusinessRuleViolationException(
                "SKU #" + variantId + " chỉ còn " + usable + " sản phẩm thuộc lô có thể bán. Vui lòng đặt lại."
            );
        }

        int remaining = needed;
        for (StockBatch batch : batches) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, batch.getRemainingQuantity());
            changeRemaining(batch.getId(), -take);
            recordAllocation(batch.getId(), orderId, operationKey, -take);
            remaining -= take;
        }
    }

    private StockBatch createBatch(long variantId, String code, int quantity) {
        StockBatch batch = new StockBatch();
        batch.setVariantId(variantId);
        batch.setBatchCode(code);
        batch.setRemainingQuantity(quantity);
        batch.setReceivedAt(LocalDateTime.now(clock));
        return stockBatchRepository.save(batch);
    }

    private void changeRemaining(long batchId, int delta) {
        int minimumBefore = Math.max(0, -delta);
        int maximumBefore = Integer.MAX_VALUE;
        if (delta > 0) {
            maximumBefore = Integer.MAX_VALUE - delta;
        }
        int changed = stockBatchRepository.changeRemaining(batchId, delta, minimumBefore, maximumBefore);
        if (changed != 1) {
            throw new IllegalStateException("Batch quantity changed concurrently");
        }
    }

    private void recordAllocation(long batchId, Long orderId, String operationKey, int delta) {
        BatchAllocation allocation = new BatchAllocation();
        allocation.setBatch(stockBatchRepository.getReferenceById(batchId));
        allocation.setOrderId(orderId);
        allocation.setOperationKey(operationKey);
        allocation.setQuantityDelta(delta);
        allocation.setCreatedAt(LocalDateTime.now(clock));
        batchAllocationRepository.save(allocation);
    }

    private String statusLabel(LocalDate expiresOn, LocalDate today) {
        if (expiresOn == null) {
            return "Chưa có hạn dùng";
        }
        if (expiresOn.isBefore(today)) {
            return "Hết hạn";
        }
        return "Còn hạn";
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 12);
    }
}
