package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;

public interface ProductQueryService {

    SellableVariantSnapshot requireSellableVariant(long variantId);
}
