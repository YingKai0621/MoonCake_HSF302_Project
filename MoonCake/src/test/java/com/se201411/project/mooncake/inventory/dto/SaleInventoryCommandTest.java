package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.entity.SalesOrder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleInventoryCommandTest {

    @Test
    void rejectsDuplicateVariantDemand() {
        List<InventoryDemand> demands = List.of(
            new InventoryDemand(11L, 2),
            new InventoryDemand(11L, 3)
        );

        SalesOrder order = new SalesOrder();
        ReflectionTestUtils.setField(order, "id", 5L);

        assertThatThrownBy(() -> new SaleInventoryCommand(order, "sale:5", demands))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unique variants");
    }

    @Test
    void rejectsUnsavedOrder() {
        List<InventoryDemand> demands = List.of(new InventoryDemand(11L, 2));

        assertThatThrownBy(() -> new SaleInventoryCommand(new SalesOrder(), "sale:5", demands))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("saved order");
    }
}
