package com.se201411.project.mooncake.product.repository.impl;

import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductAdminRepositoryImpl implements ProductAdminRepository {
    private final EntityManager em;

    public ProductAdminRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Category> categories() {
        return em.createQuery("from Category order by id", Category.class).getResultList();
    }

    @Override
    public List<Product> products() {
        return em.createQuery("from Product p join fetch p.category order by p.id", Product.class).getResultList();
    }

    @Override
    public List<ProductVariant> variants() {
        return em.createQuery("from ProductVariant v join fetch v.product order by v.id", ProductVariant.class).getResultList();
    }

    @Override
    public List<Box> boxes() {
        return em.createQuery("from Box b join fetch b.emptyBoxVariant order by b.id", Box.class).getResultList();
    }

    @Override
    public List<ProductImage> images() {
        return em.createQuery("from ProductImage i join fetch i.variant order by i.sortOrder, i.id", ProductImage.class).getResultList();
    }

    @Override
    public Category category(long id) {
        return require(Category.class, id);
    }

    @Override
    public Product product(long id) {
        return require(Product.class, id);
    }

    @Override
    public ProductVariant variant(long id) {
        return require(ProductVariant.class, id);
    }

    @Override
    public Box box(long id) {
        return require(Box.class, id);
    }

    @Override
    public ProductImage image(long id) {
        return require(ProductImage.class, id);
    }

    private <T> T require(Class<T> type, long id) {
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new ResourceNotFoundException("Không tìm thấy dữ liệu catalog.");
        return value;
    }

    @Override
    public void save(Category value) {
        if (value.getId() == null) em.persist(value);
    }

    @Override
    public void save(Product value) {
        if (value.getId() == null) em.persist(value);
    }

    @Override
    public void save(ProductVariant value) {
        if (value.getId() == null) em.persist(value);
        em.flush();
    }

    @Override
    public void save(Box value) {
        if (value.getId() == null) em.persist(value);
    }

    @Override
    public void save(ProductImage value) {
        if (value.getId() == null) em.persist(value);
    }

    @Override
    public boolean hasVariants(long id) {
        return em.createQuery("select count(v) from ProductVariant v where v.product.id=:id", Long.class).setParameter("id", id).getSingleResult() > 0;
    }

    @Override
    public boolean boxReferenced(long id) {
        return em.createQuery("select count(c) from BoxConfiguration c where c.box.id=:id", Long.class).setParameter("id", id).getSingleResult() > 0;
    }

    @Override
    public void clearPrimary(long id) {
        em.createQuery("update ProductImage i set i.primary=false where i.variant.id=:id").setParameter("id", id).executeUpdate();
        for (ProductImage image : em.createQuery("from ProductImage i where i.variant.id=:id", ProductImage.class).setParameter("id", id).getResultList())
            em.refresh(image);
    }

    @Override
    public void removeImage(ProductImage image) {
        em.remove(image);
        em.flush();
    }

    @Override
    public void flush() {
        em.flush();
    }
}
