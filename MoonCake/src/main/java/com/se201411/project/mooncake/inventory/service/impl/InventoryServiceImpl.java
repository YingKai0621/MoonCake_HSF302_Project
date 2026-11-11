package com.se201411.project.mooncake.inventory.service.impl;

import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;
import com.se201411.project.mooncake.inventory.repository.StockBatchRepository;
import com.se201411.project.mooncake.common.exception.InsufficientStockException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.dto.InventoryDemand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;
import com.se201411.project.mooncake.inventory.repository.InventoryBalanceRepository;
import com.se201411.project.mooncake.inventory.repository.StockMovementRepository;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final StockBatchRepository batchRepository;
    private final InventoryAdminRepository inventoryAdminRepository;
    private final BatchStockService batchStock;
    private final Clock clock;

    public InventoryServiceImpl(
        InventoryBalanceRepository balanceRepository,
        StockMovementRepository movementRepository,
        StockBatchRepository batchRepository,
        InventoryAdminRepository inventoryAdminRepository,
        BatchStockService batchStock,
        Clock clock
    ) {
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.batchRepository = batchRepository;
        this.inventoryAdminRepository = inventoryAdminRepository;
        this.batchStock = batchStock;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> availableQuantities(Collection<Long> variantIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (variantIds.isEmpty()) {
            return result;
        }
        for (InventoryBalance balance : balanceRepository.findByVariantIdIn(variantIds)) {
            result.put(balance.getVariantId(), balance.getAvailableQuantity());
        }
        return result;
    }

    @Override
    @Transactional
    public void initializeBalance(long variantId) {
        requireVariantId(variantId);
        balanceRepository.initializeIfMissing(variantId);
        requireBalance(variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireBalance(long variantId) {
        requireVariantId(variantId);
        if (!balanceRepository.existsById(variantId)) {
            throw new ResourceNotFoundException(
                "SKU chưa có số dư tồn kho: " + variantId
            );
        }
    }

    @Override
    @Transactional
    public void deductForSale(SaleInventoryCommand command) {
        List<InventoryDemand> demands = sortDemands(command.demands());
        for (InventoryDemand demand : demands) {
            int updatedRows = balanceRepository.deductAvailable(
                demand.variantId(),
                demand.quantity()
            );
            if (updatedRows != 1) {
                Integer available = balanceRepository.available(demand.variantId());
                int left = Objects.requireNonNullElse(available, 0);
                String product = describeVariant(demand.variantId());
                throw new InsufficientStockException(
                    "Chỉ còn " + left + " sản phẩm " + product + ". Vui lòng điều chỉnh số lượng và đặt lại."
                );
            }

            saveMovement(
                command.order(),
                demand,
                StockMovementType.SALE,
                -demand.quantity(),
                command.operationKey(),
                null
            );
        }
    }

    @Override
    @Transactional
    public void restoreForCancellation(CancellationInventoryCommand command) {
        List<InventoryDemand> demands = sortDemands(command.demands());
        for (InventoryDemand demand : demands) {
            int updatedRows = balanceRepository.restoreAvailable(
                demand.variantId(),
                demand.quantity()
            );
            if (updatedRows != 1) {
                throw new ResourceNotFoundException(
                    "Không tìm thấy số dư tồn kho cho SKU " + demand.variantId()
                );
            }

            saveMovement(
                command.order(),
                demand,
                StockMovementType.CANCEL_RETURN,
                demand.quantity(),
                command.operationKey(),
                command.performedByAccountId()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasStockRecords(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return false;
        }
        if (movementRepository.countByVariantIds(variantIds) > 0) {
            return true;
        }
        if (batchRepository.countByVariantIds(variantIds) > 0) {
            return true;
        }
        return balanceRepository.sumAvailable(variantIds) > 0;
    }

    @Override
    @Transactional
    public void deleteBalances(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return;
        }
        balanceRepository.deleteByVariantIds(variantIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Long cancelledByAccountId(long orderId) {
        List<Long> performers = movementRepository.findPerformerIds(orderId, StockMovementType.CANCEL_RETURN);
        if (performers.isEmpty()) {
            return null;
        }
        return performers.get(0);
    }

    private List<InventoryDemand> sortDemands(List<InventoryDemand> demands) {
        List<InventoryDemand> sorted = new ArrayList<>(demands);
        sorted.sort(Comparator.comparingLong(InventoryDemand::variantId));
        return sorted;
    }

    private void saveMovement(
        SalesOrder order,
        InventoryDemand demand,
        StockMovementType movementType,
        int quantityDelta,
        String operationGroupKey,
        Long performedByAccountId
    ) {
        String operationKey = operationGroupKey + ":" + demand.variantId();
        batchStock.apply(demand.variantId(), quantityDelta, operationKey, order.getId(), movementType);
        StockMovement movement = new StockMovement();
        movement.setVariant(requireBalanceRow(demand.variantId()).getVariant());
        movement.setOrder(order);
        if (performedByAccountId != null) {
            movement.setPerformedByAccount(inventoryAdminRepository.accountReference(performedByAccountId));
        }
        movement.setMovementType(movementType);
        movement.setQuantityDelta(quantityDelta);
        movement.setReason(buildReason(movementType));
        movement.setOperationGroupKey(operationGroupKey);
        movement.setOperationKey(operationKey);
        movement.setCreatedAt(LocalDateTime.now(clock));
        movementRepository.save(movement);
    }

    private String buildReason(StockMovementType movementType) {
        if (movementType == StockMovementType.SALE) {
            return "Trừ tồn khi khách đặt hàng";
        }
        return "Hoàn tồn do hủy đơn hàng";
    }

    private String describeVariant(long variantId) {
        InventoryBalance balance = balanceRepository.findById(variantId).orElse(null);
        if (balance == null) {
            return "sản phẩm đã chọn";
        }
        ProductVariant variant = balance.getVariant();

        String productName = null;
        if (variant.getProduct() != null) {
            productName = variant.getProduct().getName();
        }
        String sku = variant.getSku();

        if (productName != null && !productName.isBlank()
            && sku != null && !sku.isBlank()) {
            return productName + " (SKU: " + sku + ")";
        }
        if (productName != null && !productName.isBlank()) {
            return productName;
        }
        if (sku != null && !sku.isBlank()) {
            return "SKU " + sku;
        }
        return "sản phẩm đã chọn";
    }

    private InventoryBalance requireBalanceRow(long variantId) {
        return balanceRepository.findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("SKU chưa có số dư tồn kho: " + variantId));
    }

    private void requireVariantId(long variantId) {
        if (variantId <= 0) {
            throw new ResourceNotFoundException("SKU không hợp lệ");
        }
    }
}
