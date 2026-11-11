package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.inventory.dto.InventoryPageView;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Balance;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Movement;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockFilter;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockTab;
import java.time.LocalDate;
import java.util.List;

public interface InventoryAdminService {
    InventoryPageView inventoryPage(
        StockFilter filter,
        Long receiveVariant,
        Long packVariant,
        Long returnProduct,
        Long returnGiftBox
    );

    List<Balance> balances();

    List<Balance> balances(StockFilter filter);

    List<StockTab> stockTabs(StockFilter selected);

    List<Movement> movements(int page);

    boolean hasStockOrHistory(long variantId);

    void receive(AdminStockForm form);

    void receive(AdminStockForm form, String batchCode, LocalDate producedOn, LocalDate expiresOn);

    void adjust(AdminStockForm form);

    long countLowStockVariants();

    long countBatchesExpiringBy(LocalDate until);
}
