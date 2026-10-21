package com.se201411.project.mooncake.cart.dto;

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
}
