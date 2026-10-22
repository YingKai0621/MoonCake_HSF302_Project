package com.se201411.project.mooncake.admin;

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
    InventoryAdminServiceImpl service = new InventoryAdminServiceImpl(repo, presets, access, validation, Clock.systemUTC());
    String key = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        when(access.requireAdmin()).thenReturn(7L);
        when(repo.change(anyLong(), anyInt())).thenReturn(1);
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
        assertThatThrownBy(() -> service.receive(new AdminStockForm(1, 2, "Receive", key))).isInstanceOf(BusinessRuleViolationException.class);
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
        assertThatThrownBy(() -> service.receive(new AdminStockForm(1, -1, "Bad", key))).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> service.adjust(new AdminStockForm(1, 0, "Bad", key))).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).change(anyLong(), anyInt());
    }

    @Test
    void giftSetReceiveRequiresReadyRecipe() {
        when(repo.isGiftSet(30)).thenReturn(true);
        doThrow(new BusinessRuleViolationException("Draft")).when(presets).requireReadyPreset(30);
        assertThatThrownBy(() -> service.receive(new AdminStockForm(30, 1, "Receive", key))).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).change(anyLong(), anyInt());
    }
}
