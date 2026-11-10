package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;

import java.util.Collection;
import java.util.List;

public interface ProductQueryService {

    List<CatalogProductView> findActiveCatalog(ProductType productType);

    CatalogProductView requireActiveProduct(String slug);

    SellableVariantSnapshot requireSellableVariant(long variantId);

    List<CartVariantSnapshot> findVariantsForCart(Collection<Long> variantIds);
}
