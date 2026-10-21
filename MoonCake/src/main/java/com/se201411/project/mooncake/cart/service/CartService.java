package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CartService {

    CartSnapshot requireActiveCart(long cartId, ShopperIdentity customerIdentity);

    CheckoutCartSnapshot claimForCheckout(long cartId, ShopperIdentity customerIdentity);
}
