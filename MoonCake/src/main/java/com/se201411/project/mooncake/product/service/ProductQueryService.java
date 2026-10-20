package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;

import java.util.List;

public interface ProductQueryService {

    /**
     * Returns active products of the requested type with their sellable active variants.
     * GIFT_SET variants are included only when they have a READY PRESET recipe.
     */
    List<CatalogProductView> findActiveCatalog(ProductType productType);

    /**
     * Loads an active product by its public slug or throws a not-found exception when it
     * has no sellable variant.
     */
    CatalogProductView requireActiveProduct(String slug);

    /**
     * Resolves a SKU for Cart/Checkout without exposing a managed JPA entity.
     */
    SellableVariantSnapshot requireSellableVariant(long variantId);
}
