package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.enums.PaymentMethod;
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
    List<PaymentMethod> allowedPaymentMethods,
    PaymentMethod defaultPaymentMethod,
    List<AddressOption> provinces,
    List<AddressOption> wards
) {
    public CheckoutPageView {
        lines = List.copyOf(lines);
        allowedPaymentMethods = List.copyOf(allowedPaymentMethods);
        provinces = List.copyOf(provinces);
        wards = List.copyOf(wards);
    }

    public boolean allows(String method) {
        for (PaymentMethod allowed : allowedPaymentMethods) {
            if (allowed.name().equals(method)) {
                return true;
            }
        }
        return false;
    }
}
