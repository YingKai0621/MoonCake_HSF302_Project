package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.enums.DiscountGroup;

import java.math.BigDecimal;

public record DiscountGroupSummary(
    DiscountGroup group,
    int quantity,
    BigDecimal subtotal,
    BigDecimal discount
) {
}
