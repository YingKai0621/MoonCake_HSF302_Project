package com.se201411.project.mooncake.inventory.service.impl;

import com.se201411.project.mooncake.common.exception.InsufficientStockException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.dto.InventoryDemand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;
import com.se201411.project.mooncake.inventory.repository.InventoryBalanceRepository;
import com.se201411.project.mooncake.inventory.repository.StockMovementRepository;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.common.repository.EntityReferenceRepository;
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
    private final EntityReferenceRepository references;
    private final BatchStockService batchStock;
    private final Clock clock;

    public InventoryServiceImpl(
        InventoryBalanceRepository balanceRepository,
        StockMovementRepository movementRepository,
        EntityReferenceRepository references,
        BatchStockService batchStock,
        Clock clock
    ) {
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.references = references;
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
                command.orderId(),
                demand,
                StockMovementType.SALE,
                -demand.quantity(),
                command.operationKey()
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
                command.orderId(),
                demand,
                StockMovementType.CANCEL_RETURN,
                demand.quantity(),
                command.operationKey()
            );
        }
    }

    private List<InventoryDemand> sortDemands(List<InventoryDemand> demands) {
        List<InventoryDemand> sorted = new ArrayList<>(demands);
        sorted.sort(Comparator.comparingLong(InventoryDemand::variantId));
        return sorted;
    }

    private void saveMovement(
        long orderId,
        InventoryDemand demand,
        StockMovementType movementType,
        int quantityDelta,
        String operationGroupKey
    ) {
        String operationKey = operationGroupKey + ":" + demand.variantId();
        batchStock.apply(demand.variantId(), quantityDelta, operationKey, orderId, movementType);
        StockMovement movement = new StockMovement();
        movement.setVariant(references.variant(demand.variantId()));
        movement.setOrder(references.order(orderId));
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
        ProductVariant variant = references.findVariant(variantId).orElse(null);
        if (variant == null) {
            return "sản phẩm đã chọn";
        }

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

    private void requireVariantId(long variantId) {
        if (variantId <= 0) {
            throw new ResourceNotFoundException("SKU không hợp lệ");
        }
    }
}
