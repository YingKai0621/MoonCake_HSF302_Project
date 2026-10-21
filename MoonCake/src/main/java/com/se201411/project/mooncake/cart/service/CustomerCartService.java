package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CustomerCartService {

    CustomerCartView getCurrentCart(ShopperIdentity customerIdentity);

    int countCurrentItems(ShopperIdentity customerIdentity);

    void addVariant(ShopperIdentity customerIdentity, long variantId, int quantity);

    void updateItemQuantity(
        ShopperIdentity customerIdentity,
        long cartItemId,
        int quantity
    );

    void removeItem(ShopperIdentity customerIdentity, long cartItemId);
}
