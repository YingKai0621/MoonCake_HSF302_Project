package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.dto.OrderSuccessView;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CheckoutService {

    CheckoutPreparation prepareCheckout(ShopperIdentity customerIdentity);

    CheckoutPreparation prepareCheckout(
        long cartId,
        ShopperIdentity customerIdentity
    );

    CheckoutResult placeOrder(
        ShopperIdentity customerIdentity,
        CheckoutForm form
    );

    OrderSuccessView requireOrder(
        String orderCode,
        ShopperIdentity customerIdentity
    );
}
