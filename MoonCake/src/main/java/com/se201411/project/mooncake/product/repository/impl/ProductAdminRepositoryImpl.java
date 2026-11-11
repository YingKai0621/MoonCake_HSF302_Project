package com.se201411.project.mooncake.product.repository.impl;

import com.se201411.project.mooncake.enums.ConfigurationType;
import java.util.Collection;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
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
        return em.createQuery("from Category order by id", Category.class)
            .getResultList();
    }

    @Override
    public List<Product> products() {
        return em.createQuery("from Product p join fetch p.category order by p.id", Product.class)
            .getResultList();
    }

    @Override
    public List<ProductVariant> variants() {
        return em.createQuery("from ProductVariant v join fetch v.product order by v.id", ProductVariant.class)
            .getResultList();
    }

    @Override
    public List<Box> boxes() {
        return em.createQuery("from Box b join fetch b.emptyBoxVariant order by b.id", Box.class)
            .getResultList();
    }

    @Override
    public List<ProductImage> images() {
        String query = "from ProductImage i join fetch i.variant order by i.sortOrder, i.id";
        return em.createQuery(query, ProductImage.class)
            .getResultList();
    }

    @Override
    public Product productForView(long id) {
        List<Product> found = em.createQuery(
                "from Product p join fetch p.category where p.id = :id", Product.class)
            .setParameter("id", id)
            .getResultList();
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm.");
        }
        return found.get(0);
    }

    @Override
    public List<ProductVariant> variantsOf(long productId) {
        return em.createQuery(
                "from ProductVariant v where v.product.id = :id order by v.id", ProductVariant.class)
            .setParameter("id", productId)
            .getResultList();
    }

    @Override
    public List<ProductImage> imagesOf(long productId) {
        return em.createQuery(
                "from ProductImage i join fetch i.variant v where v.product.id = :id order by i.sortOrder, i.id",
                ProductImage.class)
            .setParameter("id", productId)
            .getResultList();
    }

    @Override
    public List<Box> boxesOf(long productId) {
        return em.createQuery(
                "from Box b join fetch b.emptyBoxVariant v where v.product.id = :id order by b.id", Box.class)
            .setParameter("id", productId)
            .getResultList();
    }

    @Override
    public List<Long> ownRecipeIds(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of();
        }
        String query = "select c.id from BoxConfiguration c where c.type = :type and c.presetVariant.id in :ids";
        return em.createQuery(query, Long.class)
            .setParameter("type", ConfigurationType.PRESET)
            .setParameter("ids", variantIds)
            .getResultList();
    }

    @Override
    public long presetComponentUsage(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return 0;
        }
        String boxQuery = "select count(c) from BoxConfiguration c where c.type = :type and c.box.emptyBoxVariant.id in :ids";
        long asBox = em.createQuery(boxQuery, Long.class)
            .setParameter("type", ConfigurationType.PRESET)
            .setParameter("ids", variantIds)
            .getSingleResult();
        String cakeQuery = "select count(i) from BoxConfigurationItem i where i.configuration.type = :type and i.mooncakeVariant.id in :ids";
        long asCake = em.createQuery(cakeQuery, Long.class)
            .setParameter("type", ConfigurationType.PRESET)
            .setParameter("ids", variantIds)
            .getSingleResult();
        return asBox + asCake;
    }

    @Override
    public boolean presetUsesBox(long boxId) {
        String query = "select count(c) from BoxConfiguration c where c.type = :type and c.box.id = :id";
        Long count = em.createQuery(query, Long.class)
            .setParameter("type", ConfigurationType.PRESET)
            .setParameter("id", boxId)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public void deleteProduct(long productId) {
        String variantIds = "select v.id from ProductVariant v where v.product.id = :id";
        String ownRecipes = "select c.id from BoxConfiguration c where c.presetVariant.product.id = :id";
        em.createQuery("delete from BoxConfigurationItem i where i.configuration.id in (" + ownRecipes + ")")
            .setParameter("id", productId)
            .executeUpdate();
        em.createQuery("delete from BoxConfiguration c where c.presetVariant.product.id = :id")
            .setParameter("id", productId)
            .executeUpdate();
        em.createQuery("delete from ProductImage i where i.variant.id in (" + variantIds + ")")
            .setParameter("id", productId)
            .executeUpdate();
        em.createQuery("delete from Box b where b.emptyBoxVariant.id in (" + variantIds + ")")
            .setParameter("id", productId)
            .executeUpdate();
        em.createQuery("delete from ProductVariant v where v.product.id = :id")
            .setParameter("id", productId)
            .executeUpdate();
        em.createQuery("delete from ProductVideo v where v.product.id = :id")
            .setParameter("id", productId)
            .executeUpdate();
        em.createQuery("delete from Product p where p.id = :id")
            .setParameter("id", productId)
            .executeUpdate();
        em.clear();
    }

    @Override
    public void deleteVariant(long variantId) {
        em.createQuery("delete from ProductImage i where i.variant.id = :id")
            .setParameter("id", variantId)
            .executeUpdate();
        em.createQuery("delete from Box b where b.emptyBoxVariant.id = :id")
            .setParameter("id", variantId)
            .executeUpdate();
        em.createQuery("delete from ProductVariant v where v.id = :id")
            .setParameter("id", variantId)
            .executeUpdate();
        em.clear();
    }

    @Override
    public boolean skuExists(String sku) {
        return em.createQuery("select count(v) from ProductVariant v where upper(v.sku) = upper(:sku)", Long.class)
            .setParameter("sku", sku)
            .getSingleResult() > 0;
    }

    @Override
    public boolean productSlugUsed(String slug, Long exceptProductId) {
        long except = exceptProductId == null ? -1 : exceptProductId;
        return em.createQuery("select count(p) from Product p where p.slug = :slug and p.id <> :except", Long.class)
            .setParameter("slug", slug)
            .setParameter("except", except)
            .getSingleResult() > 0;
    }

    @Override
    public boolean categorySlugUsed(String slug, Long exceptCategoryId) {
        long except = exceptCategoryId == null ? -1 : exceptCategoryId;
        return em.createQuery("select count(c) from Category c where c.slug = :slug and c.id <> :except", Long.class)
            .setParameter("slug", slug)
            .setParameter("except", except)
            .getSingleResult() > 0;
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
        if (value == null) {
            throw new ResourceNotFoundException("Không tìm thấy dữ liệu, có thể đã bị xóa. Hãy tải lại trang.");
        }
        return value;
    }

    @Override
    public void save(Category value) {
        if (value.getId() == null) {
            em.persist(value);
        }
    }

    @Override
    public void save(Product value) {
        if (value.getId() == null) {
            em.persist(value);
        }
    }

    @Override
    public void save(ProductVariant value) {
        if (value.getId() == null) {
            em.persist(value);
        }
        em.flush();
    }

    @Override
    public void save(Box value) {
        if (value.getId() == null) {
            em.persist(value);
        }
    }

    @Override
    public void save(ProductImage value) {
        if (value.getId() == null) {
            em.persist(value);
        }
    }

    @Override
    public boolean hasVariants(long id) {
        Long count = em.createQuery("select count(v) from ProductVariant v where v.product.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public void clearPrimary(long id) {
        em.createQuery("update ProductImage i set i.primary = false where i.variant.id = :id")
            .setParameter("id", id)
            .executeUpdate();
        List<ProductImage> images = em.createQuery("from ProductImage i where i.variant.id = :id", ProductImage.class)
            .setParameter("id", id)
            .getResultList();
        for (ProductImage image : images) {
            em.refresh(image);
        }
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
