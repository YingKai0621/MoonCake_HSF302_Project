package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;

import java.util.List;

public record AdminPresetView(long id, long giftSetVariantId, String sku, long boxId,
                              int capacity, ConfigurationStatus status, List<Component> components,
                              String giftName, String boxName) {
    public AdminPresetView {
        components = List.copyOf(components);
    }

    public int filled() {
        int total = 0;
        for (Component component : components) {
            total += component.quantity();
        }
        return total;
    }

    public boolean draft() {
        return status == ConfigurationStatus.DRAFT;
    }

    public int fillPercent() {
        if (capacity <= 0) {
            return 0;
        }
        return Math.min(100, filled() * 100 / capacity);
    }

    public record Component(long variantId, String sku, int quantity, String name) {

        /** Entity -> DTO. The service builds the cake display name and passes it in. */
        public static Component entityToDto(BoxConfigurationItem item, String name) {
            ProductVariant cake = item.getMooncakeVariant();
            return new Component(
                cake.getId(),
                cake.getSku(),
                item.getQuantityPerBox(),
                name
            );
        }
    }
}
