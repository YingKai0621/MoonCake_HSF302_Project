package com.se201411.project.mooncake.common.repository.impl;

import com.se201411.project.mooncake.common.repository.EntityReferenceRepository;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.entity.VolumeDiscountTier;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EntityReferenceRepositoryImpl implements EntityReferenceRepository {

    private final EntityManager em;

    public EntityReferenceRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public ProductVariant variant(long id) {
        return em.getReference(ProductVariant.class, id);
    }

    @Override
    public Optional<ProductVariant> findVariant(long id) {
        ProductVariant variant = em.find(ProductVariant.class, id);
        return Optional.ofNullable(variant);
    }

    @Override
    public SalesOrder order(long id) {
        return em.getReference(SalesOrder.class, id);
    }

    @Override
    public Customer customer(long id) {
        return em.getReference(Customer.class, id);
    }

    @Override
    public BoxConfiguration configuration(long id) {
        return em.getReference(BoxConfiguration.class, id);
    }

    @Override
    public VolumeDiscountTier discountTier(long id) {
        return em.getReference(VolumeDiscountTier.class, id);
    }
}
