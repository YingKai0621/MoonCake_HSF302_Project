package com.se201411.project.mooncake.inventory.service;

import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.*;

import java.util.List;

public interface InventoryAdminService {
    List<Balance> balances();

    List<Movement> movements(int page);

    boolean hasStockOrHistory(long variantId);

    void receive(AdminStockForm form);

    void adjust(AdminStockForm form);
}
