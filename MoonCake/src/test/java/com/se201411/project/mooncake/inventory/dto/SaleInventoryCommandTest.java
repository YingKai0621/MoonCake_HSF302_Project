package com.se201411.project.mooncake.inventory.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleInventoryCommandTest {

    @Test
    void rejectsDuplicateVariantDemand() {
        List<InventoryDemand> demands = List.of(
            new InventoryDemand(11L, 2),
            new InventoryDemand(11L, 3)
        );

        assertThatThrownBy(() -> new SaleInventoryCommand(5L, "sale:5", demands))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unique variants");
    }
}
