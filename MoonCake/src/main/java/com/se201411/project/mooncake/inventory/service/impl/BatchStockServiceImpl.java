package com.se201411.project.mooncake.inventory.service.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.common.exception.InsufficientStockException;
import com.se201411.project.mooncake.inventory.dto.VariantQuantity;
import com.se201411.project.mooncake.common.format.VietnamTime;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BatchStockServiceImpl implements BatchStockService {
    private static final int EXPIRY_WARNING_DAYS = 14;
    private static final int MAX_CODE_LENGTH = 80;
    private static final String OPENING_CODE = "OPENING";
    private static final DateTimeFormatter BATCH_DAY = DateTimeFormatter.ofPattern("yyMMdd");

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
            createBatch(variantId, newBatchCode(), before - tracked);
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
    public void finishPack(String operationGroup, long variantId, LocalDate packedOn, LocalDate shelfLifeLimit) {
        LocalDate earliestExpiry = batchAllocationRepository.findEarliestExpiryConsumed(
            operationGroup + ":",
            ProductType.MOONCAKE
        );
        LocalDate expiresOn = earlierOf(earliestExpiry, shelfLifeLimit);
        String outputKey = operationGroup + ":" + variantId + ":IN";
        for (StockBatch batch : batchAllocationRepository.findBatchesCreatedBy(outputKey)) {
            batch.setProducedOn(packedOn);
            batch.setExpiresOn(expiresOn);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long variantIdOf(long batchId) {
        StockBatch batch = stockBatchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng."));
        return batch.getVariantId();
    }

    private LocalDate earlierOf(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null || first.isBefore(second)) {
            return first;
        }
        return second;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBatchView> list() {
        adminAccessService.requireAdmin();
        LocalDate today = VietnamTime.today(clock);
        List<AdminBatchView> views = new ArrayList<>();
        for (StockBatch batch : stockBatchRepository.findAllByOrderByIdDesc()) {
            String codeLabel = batchCodeLabel(batch.getBatchCode());
            String status = statusLabel(batch.getExpiresOn(), today);
            String tone = statusTone(batch.getExpiresOn(), today);
            views.add(AdminBatchView.entityToDto(batch, codeLabel, status, tone));
        }
        return views;
    }

    @Override
    @Transactional
    public void updateTraceability(long batchId, String code, LocalDate producedOn, LocalDate expiresOn) {
        adminAccessService.requireAdmin();
        boolean invalidCode = code == null || code.isBlank() || code.length() > MAX_CODE_LENGTH;
        if (invalidCode) {
            throw new BusinessRuleViolationException("Mã lô tối đa 80 ký tự.");
        }
        requireValidDates(producedOn, expiresOn);
        int changed = stockBatchRepository.updateTraceability(batchId, code.trim(), producedOn, expiresOn);
        if (changed != 1) {
            throw new BusinessRuleViolationException("Lô đã xuất hàng không thể đổi thông tin truy xuất.");
        }
    }

    @Override
    @Transactional
    public void updateCode(long batchId, String code) {
        StockBatch batch = stockBatchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng."));
        updateTraceability(batchId, code, batch.getProducedOn(), batch.getExpiresOn());
    }

    @Override
    @Transactional
    public void labelReceivedBatch(String operationKey, String code, LocalDate producedOn, LocalDate expiresOn) {
        boolean hasCode = code != null && !code.isBlank();
        if (hasCode && code.trim().length() > MAX_CODE_LENGTH) {
            throw new BusinessRuleViolationException("Mã lô tối đa 80 ký tự.");
        }
        requireValidDates(producedOn, expiresOn);
        for (StockBatch batch : batchAllocationRepository.findBatchesCreatedBy(operationKey)) {
            if (hasCode) {
                batch.setBatchCode(code.trim());
            }
            batch.setProducedOn(producedOn);
            batch.setExpiresOn(expiresOn);
        }
    }

    private void requireValidDates(LocalDate producedOn, LocalDate expiresOn) {
        if (producedOn != null && producedOn.isAfter(VietnamTime.today(clock))) {
            throw new BusinessRuleViolationException("Ngày sản xuất không được sau hôm nay.");
        }
        if (producedOn != null && expiresOn != null && expiresOn.isBefore(producedOn)) {
            throw new BusinessRuleViolationException("Hạn dùng phải sau ngày sản xuất.");
        }
    }

    private void addStock(long variantId, int quantity, String operationKey, Long orderId, StockMovementType type) {
        List<BatchQuantity> originalBatches = List.of();
        if (type == StockMovementType.CANCEL_RETURN && orderId != null) {
            originalBatches = batchAllocationRepository.findSoldForOrder(variantId, orderId);
        }

        if (originalBatches.isEmpty()) {
            StockBatch batch = createBatch(variantId, newBatchCode(), quantity);
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
            batches = stockBatchRepository.findSellableForUpdate(variantId, VietnamTime.today(clock));
        }

        long usable = 0;
        for (StockBatch batch : batches) {
            usable += batch.getRemainingQuantity();
        }
        if (usable < needed && type == StockMovementType.SALE) {
            throw new InsufficientStockException(
                variantName(variantId) + ": vượt quá số lượng sản phẩm trong kho. Vui lòng giảm số lượng và đặt lại."
            );
        }
        if (usable < needed) {
            throw new InsufficientStockException(
                "Chỉ còn " + usable + " " + variantName(variantId) + " còn hạn sử dụng."
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
        if (!expiresOn.isAfter(today.plusDays(EXPIRY_WARNING_DAYS))) {
            return "Sắp hết hạn";
        }
        return "Còn hạn";
    }

    private String statusTone(LocalDate expiresOn, LocalDate today) {
        if (expiresOn == null) {
            return "muted";
        }
        if (expiresOn.isBefore(today)) {
            return "danger";
        }
        if (!expiresOn.isAfter(today.plusDays(EXPIRY_WARNING_DAYS))) {
            return "warning";
        }
        return "success";
    }

    private String newBatchCode() {
        String day = VietnamTime.today(clock).format(BATCH_DAY);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return "LO-" + day + "-" + suffix;
    }

    private String batchCodeLabel(String code) {
        if (OPENING_CODE.equals(code)) {
            return "Tồn đầu kỳ";
        }
        return code;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> expiredQuantities(Collection<Long> variantIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (variantIds.isEmpty()) {
            return result;
        }
        LocalDate today = VietnamTime.today(clock);
        for (VariantQuantity expired : stockBatchRepository.sumExpiredRemaining(variantIds, today)) {
            result.put(expired.variantId(), Math.toIntExact(expired.quantity()));
        }
        return result;
    }

    private String variantName(long variantId) {
        InventoryBalance balance = inventoryBalanceRepository.findById(variantId).orElse(null);
        if (balance == null) {
            return "sản phẩm";
        }
        return balance.getVariant().getProduct().getName() + " (" + balance.getVariant().getName() + ")";
    }
}
