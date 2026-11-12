package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.common.exception.InsufficientStockException;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.BatchAllocation;
import com.se201411.project.mooncake.entity.StockBatch;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.BatchQuantity;
import com.se201411.project.mooncake.inventory.repository.BatchAllocationRepository;
import com.se201411.project.mooncake.inventory.repository.InventoryBalanceRepository;
import com.se201411.project.mooncake.inventory.repository.StockBatchRepository;
import com.se201411.project.mooncake.inventory.service.impl.BatchStockServiceImpl;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchStockServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    private final StockBatchRepository stockBatchRepository = mock(StockBatchRepository.class);
    private final BatchAllocationRepository batchAllocationRepository = mock(BatchAllocationRepository.class);
    private final InventoryBalanceRepository inventoryBalanceRepository = mock(InventoryBalanceRepository.class);
    private final BatchStockService service = new BatchStockServiceImpl(
        stockBatchRepository,
        batchAllocationRepository,
        inventoryBalanceRepository,
        mock(AdminAccessService.class),
        Clock.fixed(Instant.parse("2026-11-09T03:00:00Z"), ZoneOffset.UTC)
    );

    @BeforeEach
    void changesSucceedByDefault() {
        when(stockBatchRepository.changeRemaining(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
    }

    @Test
    void saleConsumesEligibleBatchesInExpiryOrder() {
        when(inventoryBalanceRepository.available(1)).thenReturn(7);
        when(stockBatchRepository.sumRemaining(1)).thenReturn(12L);
        when(stockBatchRepository.findSellableForUpdate(1, TODAY)).thenReturn(List.of(batch(11L, 3), batch(12L, 9)));

        service.apply(1, -5, "SALE", 9L, StockMovementType.SALE);

        InOrder order = inOrder(stockBatchRepository);
        order.verify(stockBatchRepository).changeRemaining(11L, -3, 3, Integer.MAX_VALUE);
        order.verify(stockBatchRepository).changeRemaining(12L, -2, 2, Integer.MAX_VALUE);
        verify(batchAllocationRepository, times(2)).save(any(BatchAllocation.class));
    }

    @Test
    void insufficientEligibleStockDoesNotConsumeAnyBatch() {
        when(inventoryBalanceRepository.available(1)).thenReturn(7);
        when(stockBatchRepository.sumRemaining(1)).thenReturn(12L);
        when(stockBatchRepository.findSellableForUpdate(1, TODAY)).thenReturn(List.of(batch(11L, 3)));

        assertThatThrownBy(() -> service.apply(1, -5, "SALE", 9L, StockMovementType.SALE))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("vượt quá số lượng");
        verify(stockBatchRepository, never()).changeRemaining(anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void cancellationRestoresOriginalLotsEvenWhenExpired() {
        when(inventoryBalanceRepository.available(1)).thenReturn(12);
        when(stockBatchRepository.sumRemaining(1)).thenReturn(7L);
        when(batchAllocationRepository.findSoldForOrder(1, 9))
            .thenReturn(List.of(new BatchQuantity(11L, 3L), new BatchQuantity(12L, 2L)));

        service.apply(1, 5, "RETURN", 9L, StockMovementType.CANCEL_RETURN);

        verify(stockBatchRepository).changeRemaining(11L, 3, 0, Integer.MAX_VALUE - 3);
        verify(stockBatchRepository).changeRemaining(12L, 2, 0, Integer.MAX_VALUE - 2);
        verify(stockBatchRepository, never()).save(any(StockBatch.class));
    }

    @Test
    void balanceMismatchFailsInsteadOfFabricatingLots() {
        when(inventoryBalanceRepository.available(1)).thenReturn(2);
        when(stockBatchRepository.sumRemaining(1)).thenReturn(9L);

        assertThatThrownBy(() -> service.apply(1, -1, "SALE", 9L, StockMovementType.SALE))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    private StockBatch batch(long id, int remaining) {
        StockBatch batch = new StockBatch();
        ReflectionTestUtils.setField(batch, "id", id);
        batch.setRemainingQuantity(remaining);
        return batch;
    }

    @Test
    void productionDateInTheFutureIsRejected() {
        LocalDate tomorrow = LocalDate.of(2026, 11, 10);

        assertThatThrownBy(() -> service.labelReceivedBatch("RECEIVE:1", "LO-1", tomorrow, tomorrow.plusDays(30)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Ngày sản xuất không được sau hôm nay.");
        assertThatThrownBy(() -> service.updateTraceability(1L, "LO-1", tomorrow, tomorrow.plusDays(30)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Ngày sản xuất không được sau hôm nay.");
    }

    @Test
    void todayInVietnamIsAcceptedBeforeSevenInTheMorning() {
        BatchStockService earlyMorning = new BatchStockServiceImpl(
            stockBatchRepository,
            batchAllocationRepository,
            inventoryBalanceRepository,
            mock(AdminAccessService.class),
            Clock.fixed(Instant.parse("2026-11-08T23:00:00Z"), ZoneOffset.UTC)
        );
        LocalDate vietnamToday = LocalDate.of(2026, 11, 9);
        when(batchAllocationRepository.findBatchesCreatedBy("RECEIVE:1")).thenReturn(List.of());

        assertThatCode(() -> earlyMorning.labelReceivedBatch("RECEIVE:1", "LO-1", vietnamToday, vietnamToday.plusDays(30)))
            .doesNotThrowAnyException();
    }

    @Test
    void packedGiftBoxExpiresWithTheEarliestCakeInside() {
        StockBatch giftBatch = batch(50, 2);
        LocalDate packedOn = LocalDate.of(2026, 11, 9);
        when(batchAllocationRepository.findEarliestExpiryConsumed(eq("PACK:1:"), any())).thenReturn(LocalDate.of(2026, 11, 25));
        when(batchAllocationRepository.findBatchesCreatedBy("PACK:1:30:IN")).thenReturn(List.of(giftBatch));

        service.finishPack("PACK:1", 30, packedOn);

        assertThat(giftBatch.getProducedOn()).isEqualTo(packedOn);
        assertThat(giftBatch.getExpiresOn()).isEqualTo(LocalDate.of(2026, 11, 25));
    }
}
