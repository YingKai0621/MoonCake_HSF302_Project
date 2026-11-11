package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.VolumeDiscountTier;

public interface PricingService {

    CheckoutQuote calculateCheckoutQuote(CheckoutCartSnapshot cart);

    BoxConfiguration configurationReference(long configurationId);

    VolumeDiscountTier discountTierReference(long tierId);
}
