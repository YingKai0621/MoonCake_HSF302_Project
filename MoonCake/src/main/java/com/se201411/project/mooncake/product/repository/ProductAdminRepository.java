package com.se201411.project.mooncake.product.repository;

import java.util.Collection;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import java.util.List;

public interface ProductAdminRepository {

    List<Category> categories();

    List<Product> products();

    List<ProductVariant> variants();

    List<Box> boxes();

    List<ProductImage> images();

    Product productForView(long id);

    List<ProductVariant> variantsOf(long productId);

    List<ProductImage> imagesOf(long productId);

    List<Box> boxesOf(long productId);

    List<Long> ownRecipeIds(Collection<Long> variantIds);

    long presetComponentUsage(Collection<Long> variantIds);

    void deleteProduct(long productId);

    void deleteVariant(long variantId);

    boolean skuExists(String sku);

    boolean productSlugUsed(String slug, Long exceptProductId);

    boolean categorySlugUsed(String slug, Long exceptCategoryId);

    Category category(long id);

    Product product(long id);

    ProductVariant variant(long id);

    Box box(long id);

    ProductImage image(long id);

    void save(Category value);

    void save(Product value);

    void save(ProductVariant value);

    void save(Box value);

    void save(ProductImage value);

    boolean hasVariants(long id);

    boolean presetUsesBox(long boxId);

    void clearPrimary(long id);

    void removeImage(ProductImage image);

    void flush();
}
