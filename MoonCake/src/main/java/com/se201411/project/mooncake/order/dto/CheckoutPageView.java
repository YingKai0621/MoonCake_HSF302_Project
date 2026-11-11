package com.se201411.project.mooncake.order.dto;

import java.util.List;

public record CheckoutPageView(
    long cartId,
    List<CheckoutLineView> lines,
    int totalQuantity,
    String formattedSubtotal,
    String formattedDiscount,
    String formattedShippingFee,
    String formattedRoundingAdjustment,
    String formattedTotal,
    boolean codAllowed,
    List<AddressOption> provinces,
    List<AddressOption> wards
) {
    public CheckoutPageView {
        lines = List.copyOf(lines);
        provinces = List.copyOf(provinces);
        wards = List.copyOf(wards);
    }
}
