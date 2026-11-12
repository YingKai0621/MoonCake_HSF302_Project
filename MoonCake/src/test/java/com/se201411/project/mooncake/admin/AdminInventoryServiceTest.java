package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import com.se201411.project.mooncake.inventory.service.impl.InventoryAdminServiceImpl;
import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.*;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AdminInventoryServiceTest {
    InventoryAdminRepository repo = mock(InventoryAdminRepository.class);
    PresetConfigurationQueryService presets = mock(PresetConfigurationQueryService.class);
    AdminAccessService access = mock(AdminAccessService.class);
    AdminFormValidation validation = mock(AdminFormValidation.class);
    BatchStockService batches = mock(BatchStockService.class);
    ProductQueryService products = mock(ProductQueryService.class);
    InventoryAdminServiceImpl service = new InventoryAdminServiceImpl(repo, presets, access, validation, batches, products, Clock.systemUTC());
    String key = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        when(access.requireAdmin()).thenReturn(7L);
        when(repo.change(anyLong(), anyInt())).thenReturn(1);
        when(products.shelfLifeDays(anyLong())).thenReturn(null);
    }

    @Test
    void receiveLabelsTheNewBatchWithCodeAndDates() {
        LocalDate produced = LocalDate.now().minusDays(2);
        LocalDate expiry = produced.plusDays(30);

        when(products.shelfLifeDays(11)).thenReturn(30);

        service.receive(new AdminStockForm(11, 20, "Nhập xưởng", key), "LO-0901", produced);

        verify(repo).change(11, 20);
        verify(batches).labelReceivedBatch("RECEIVE:" + key + ":11", "LO-0901", produced, expiry);
    }

    @Test
    void receiveWithoutBatchInfoKeepsTheAutomaticBatch() {
        service.receive(new AdminStockForm(11, 20, "Nhập xưởng", key), " ", null);

        verify(batches, never()).labelReceivedBatch(anyString(), any(), any(), any());
    }

    @Test
    void packAggregatesComponentsAndWritesGroupedMovements() {
        when(presets.requireReadyPreset(30)).thenReturn(new PresetRecipeSnapshot(1, 30, 10, 4, ConfigurationStatus.READY,
                List.of(new PresetComponentSnapshot(20, 1), new PresetComponentSnapshot(20, 3))));
        service.pack(new AdminStockForm(30, 2, "Pack", key));
        verify(repo).change(10, -2);
        verify(repo).change(20, -8);
        verify(repo).change(30, 2);
        verify(repo, times(3)).movement(anyLong(), eq(7L), anyInt(), eq(StockMovementType.PACK), eq("Pack"), eq("PACK:" + key), anyString(), any());
        verify(repo).movement(eq(30L), eq(7L), eq(2), eq(StockMovementType.PACK), eq("Pack"), eq("PACK:" + key), eq("PACK:" + key + ":30:IN"), any());
    }

    @Test
    void repeatedOperationStopsBeforeStockChange() {
        when(repo.operationExists("RECEIVE:" + key)).thenReturn(true);
        assertThatThrownBy(() -> service.receive(new AdminStockForm(1, 2, "Receive", key), null, null)).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).change(anyLong(), anyInt());
    }

    @Test
    void insufficientStockStopsPacking() {
        when(presets.requireReadyPreset(30)).thenReturn(new PresetRecipeSnapshot(1, 30, 10, 4, ConfigurationStatus.READY, List.of(new PresetComponentSnapshot(20, 4))));
        when(repo.change(10, -2)).thenReturn(0);
        assertThatThrownBy(() -> service.pack(new AdminStockForm(30, 2, "Pack", key))).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).change(30, 2);
        verify(repo, never()).movement(anyLong(), anyLong(), anyInt(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void receiveRejectsNegativeAndAdjustmentRejectsZero() {
        assertThatThrownBy(() -> service.receive(new AdminStockForm(1, -1, "Bad", key), null, null)).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> service.adjust(new AdminStockForm(1, 0, "Bad", key))).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).change(anyLong(), anyInt());
    }

    @Test
    void giftSetCannotBeReceivedDirectly() {
        when(repo.isGiftSet(30)).thenReturn(true);

        assertThatThrownBy(() -> service.receive(new AdminStockForm(30, 1, "Receive", key), null, LocalDate.now()))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Đóng hộp quà");
        verify(repo, never()).change(anyLong(), anyInt());
    }

    @Test
    void emptyBoxReceiveIgnoresProductionDate() {
        when(products.shelfLifeDays(12)).thenReturn(null);

        service.receive(new AdminStockForm(12, 5, "Nhập vỏ hộp", key), "LO-HOP", LocalDate.now().minusDays(3));

        verify(repo).change(12, 5);
        verify(batches).labelReceivedBatch("RECEIVE:" + key + ":12", "LO-HOP", null, null);
    }

    @Test
    void editingPackedGiftBatchOnlyChangesTheCode() {
        when(batches.variantIdOf(9L)).thenReturn(30L);
        when(repo.isGiftSet(30)).thenReturn(true);
        when(products.shelfLifeDays(30)).thenReturn(20);

        service.updateBatchInfo(9L, "LO-QUA", LocalDate.of(2026, 9, 1));

        verify(batches).updateCode(9L, "LO-QUA");
        verify(batches, never()).updateTraceability(anyLong(), anyString(), any(), any());
    }

    @Test
    void editingEmptyBoxBatchOnlyChangesTheCode() {
        when(batches.variantIdOf(9L)).thenReturn(12L);
        when(products.shelfLifeDays(12)).thenReturn(null);

        service.updateBatchInfo(9L, "LO-HOP", LocalDate.of(2026, 9, 1));

        verify(batches).updateCode(9L, "LO-HOP");
        verify(batches, never()).updateTraceability(anyLong(), anyString(), any(), any());
    }

    @Test
    void cakeWithoutProductionDateIsRejectedBeforeStockChanges() {
        when(products.shelfLifeDays(11)).thenReturn(30);

        assertThatThrownBy(() -> service.receive(new AdminStockForm(11, 20, "Nhập xưởng", key), "LO-1", null))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Vui lòng nhập ngày sản xuất.");
        verify(repo, never()).change(anyLong(), anyInt());
    }

    @Test
    void emptyBoxNeedsNoProductionDate() {
        when(products.shelfLifeDays(12)).thenReturn(null);

        service.receive(new AdminStockForm(12, 5, "Nhập vỏ hộp", key), null, null);

        verify(repo).change(12, 5);
    }

    @Test
    void packingLabelsTheGiftBatchWithTheVietnamPackDate() {
        when(presets.requireReadyPreset(30)).thenReturn(new PresetRecipeSnapshot(1, 30, 10, 4, ConfigurationStatus.READY,
                List.of(new PresetComponentSnapshot(20, 4))));

        service.pack(new AdminStockForm(30, 1, "Pack", key));

        verify(batches).finishPack(eq("PACK:" + key), eq(30L), any(LocalDate.class));
    }

    @Test
    void editingBatchRecomputesExpiryFromProductionDate() {
        when(batches.variantIdOf(9L)).thenReturn(11L);
        when(products.shelfLifeDays(11)).thenReturn(45);
        LocalDate produced = LocalDate.of(2026, 9, 1);

        service.updateBatchInfo(9L, "LO-9", produced);

        verify(batches).updateTraceability(9L, "LO-9", produced, produced.plusDays(45));
    }

    @Test
    void alreadyExpiredLotCannotBeReceived() {
        when(products.shelfLifeDays(11)).thenReturn(30);
        LocalDate produced = LocalDate.now().minusDays(60);

        assertThatThrownBy(() -> service.receive(new AdminStockForm(11, 5, "Nhập lô cũ", key), "LO-OLD", produced))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đã hết hạn");
        verify(repo, never()).change(anyLong(), anyInt());
    }

    @Test
    void writingOffExpiredStockAdjustsByTheExpiredQuantity() {
        when(batches.expiredQuantities(List.of(11L))).thenReturn(Map.of(11L, 4));

        service.writeOffExpired(11L, key);

        verify(repo).change(11, -4);
        verify(repo).movement(eq(11L), eq(7L), eq(-4), eq(StockMovementType.ADJUST), eq("Hủy hàng hết hạn"), eq("ADJUST:" + key), eq("ADJUST:" + key + ":11"), any());
    }

    @Test
    void writingOffWithoutExpiredStockIsRejected() {
        when(batches.expiredQuantities(List.of(11L))).thenReturn(Map.of());

        assertThatThrownBy(() -> service.writeOffExpired(11L, key))
            .isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).change(anyLong(), anyInt());
    }
}
