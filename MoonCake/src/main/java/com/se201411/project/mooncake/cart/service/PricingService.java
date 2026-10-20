package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;

public interface PricingService {

    CheckoutQuote calculateCheckoutQuote(CheckoutCartSnapshot cart);
}
