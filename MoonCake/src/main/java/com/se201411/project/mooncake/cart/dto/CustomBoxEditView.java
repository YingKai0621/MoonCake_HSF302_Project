package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.CartItem;

import java.util.Map;

public record CustomBoxEditView(
    long configurationId,
    long boxId,
    int boxQuantity,
    Map<Long, Integer> cakeQuantities
) {
    public CustomBoxEditView {
        cakeQuantities = Map.copyOf(cakeQuantities);
    }

    public static CustomBoxEditView entityToDto(
        BoxConfiguration configuration,
        CartItem cartItem,
        Map<Long, Integer> cakeQuantities
    ) {
        return new CustomBoxEditView(
            configuration.getId(),
            configuration.getBox().getId(),
            cartItem.getQuantity(),
            cakeQuantities
        );
    }
}
