package com.se201411.project.mooncake.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuote(
    List<CheckoutLineQuote> lines,
    List<DiscountGroupSummary> groupSummaries,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal shippingFee,
    BigDecimal total
) {
    public CheckoutQuote {
        lines = List.copyOf(lines);
        groupSummaries = List.copyOf(groupSummaries);
    }
}
