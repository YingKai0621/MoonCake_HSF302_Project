package com.se201411.project.mooncake.cart.dto;

import java.util.List;

public record CustomBoxPageView(
    List<CustomBoxOptionView> boxes,
    List<CustomCakeOptionView> cakes
) {
    public CustomBoxPageView {
        boxes = List.copyOf(boxes);
        cakes = List.copyOf(cakes);
    }
}
