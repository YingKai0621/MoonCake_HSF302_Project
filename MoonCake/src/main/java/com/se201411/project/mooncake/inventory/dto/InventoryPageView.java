package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Balance;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockFilter;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockTab;

import java.util.List;

public record InventoryPageView(
    StockFilter filter,
    List<StockTab> tabs,
    List<Balance> balances,
    List<Balance> packableGiftBoxes,
    String receiveKey,
    String adjustKey,
    String packKey,
    Long receiveVariant,
    Long packVariant,
    Long returnProduct,
    Long returnGiftBox
) {
    public InventoryPageView {
        tabs = List.copyOf(tabs);
        balances = List.copyOf(balances);
        packableGiftBoxes = List.copyOf(packableGiftBoxes);
    }
}
