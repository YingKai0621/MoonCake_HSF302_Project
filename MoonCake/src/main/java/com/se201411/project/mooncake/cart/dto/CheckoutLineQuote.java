package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.enums.DiscountGroup;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutLineQuote(
    CartLineSnapshot cartLine,
    String productName,
    String sku,
    DiscountGroup discountGroup,
    BigDecimal unitBasePrice,
    BigDecimal discountPercent,
    BigDecimal unitDiscount,
    BigDecimal unitFinalPrice,
    BigDecimal lineTotal,
    List<CheckoutComponentQuote> components
) {
    public CheckoutLineQuote {
        components = List.copyOf(components);
    }
}
