package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.common.exception.InsufficientStockException;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.inventory.dto.InventoryDemand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;
import com.se201411.project.mooncake.inventory.repository.InventoryBalanceRepository;
import com.se201411.project.mooncake.inventory.repository.StockMovementRepository;
import com.se201411.project.mooncake.inventory.service.impl.InventoryServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryBalanceRepository balanceRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private EntityManager entityManager;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC);
        inventoryService = new InventoryServiceImpl(
            balanceRepository,
            movementRepository,
            entityManager,
            clock
        );
    }

    @Test
    void deductsConditionallyAndCreatesSaleMovement() {
        ProductVariant variant = new ProductVariant();
        SalesOrder order = new SalesOrder();
        when(balanceRepository.deductAvailable(5L, 3)).thenReturn(1);
        when(entityManager.getReference(ProductVariant.class, 5L)).thenReturn(variant);
        when(entityManager.getReference(SalesOrder.class, 8L)).thenReturn(order);

        inventoryService.deductForSale(new SaleInventoryCommand(
            8L,
            "SALE:8",
            List.of(new InventoryDemand(5L, 3))
        ));

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(
            StockMovement.class
        );
        verify(movementRepository).save(movementCaptor.capture());
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getQuantityDelta()).isEqualTo(-3);
        assertThat(movement.getOperationKey()).isEqualTo("SALE:8:5");
        assertThat(movement.getOperationGroupKey()).isEqualTo("SALE:8");
    }

    @Test
    void insufficientStockDoesNotCreateMovement() {
        when(balanceRepository.deductAvailable(5L, 3)).thenReturn(0);
        Product product = new Product();
        product.setName("Bánh Chà Bông Trứng Nghiền");
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("MC-CBTN-080");
        when(entityManager.find(ProductVariant.class, 5L)).thenReturn(variant);

        SaleInventoryCommand command = new SaleInventoryCommand(
            8L,
            "SALE:8",
            List.of(new InventoryDemand(5L, 3))
        );

        assertThatThrownBy(() -> inventoryService.deductForSale(command))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessage(
                "Không đủ tồn kho cho Bánh Chà Bông Trứng Nghiền (SKU: MC-CBTN-080)"
            );

        verify(movementRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
