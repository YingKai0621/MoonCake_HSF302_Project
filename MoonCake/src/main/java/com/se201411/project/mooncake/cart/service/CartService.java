package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

import java.util.Collection;

public interface CartService {

    CheckoutCartSnapshot requireCurrentActiveCart(ShopperIdentity customerIdentity);

    CartSnapshot requireActiveCart(long cartId, ShopperIdentity customerIdentity);

    CheckoutCartSnapshot claimForCheckout(long cartId, ShopperIdentity customerIdentity);

    long countItemsWithVariants(Collection<Long> variantIds);

    long countItemsWithConfigurations(Collection<Long> configurationIds);
}
