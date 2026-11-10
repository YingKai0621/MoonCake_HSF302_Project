package com.se201411.project.mooncake.common.repository;

import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.entity.VolumeDiscountTier;

import java.util.Optional;

public interface EntityReferenceRepository {

    ProductVariant variant(long id);

    Optional<ProductVariant> findVariant(long id);

    SalesOrder order(long id);

    Customer customer(long id);

    BoxConfiguration configuration(long id);

    VolumeDiscountTier discountTier(long id);
}
