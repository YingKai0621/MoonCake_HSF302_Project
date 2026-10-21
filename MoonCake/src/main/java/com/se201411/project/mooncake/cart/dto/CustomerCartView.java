package com.se201411.project.mooncake.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerCartView(
    Long cartId,
    List<CartItemView> items,
    int totalQuantity,
    BigDecimal subtotal,
    String formattedSubtotal,
    boolean hasUnavailableItems
) {
    public CustomerCartView {
        items = List.copyOf(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public static CustomerCartView empty() {
        return new CustomerCartView(
            null,
            List.of(),
            0,
            BigDecimal.ZERO,
            "0đ",
            false
        );
    }
}
