package com.se201411.project.mooncake.cart.dto;

import java.util.List;

public record SaveCustomBoxCommand(
    long boxId,
    int boxQuantity,
    List<CustomCakeQuantity> cakes
) {
    public SaveCustomBoxCommand {
        cakes = List.copyOf(cakes);
    }
}
