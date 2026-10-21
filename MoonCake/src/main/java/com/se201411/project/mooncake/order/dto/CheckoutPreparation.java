package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.security.dto.CustomerProfile;

public record CheckoutPreparation(
    long cartId,
    CustomerProfile customer,
    CheckoutQuote quote
) {
}
