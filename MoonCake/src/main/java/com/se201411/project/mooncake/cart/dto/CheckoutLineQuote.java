package com.se201411.project.mooncake.cart.dto;

import com.se201411.project.mooncake.enums.DiscountGroup;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderItemKind;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutLineQuote(
    CartLineSnapshot cartLine,
    Long volumeDiscountTierId,
    OrderItemKind itemKind,
    InventorySource inventorySource,
    String productName,
    String sku,
    DiscountGroup discountGroup,
    String boxName,
    String boxSku,
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
