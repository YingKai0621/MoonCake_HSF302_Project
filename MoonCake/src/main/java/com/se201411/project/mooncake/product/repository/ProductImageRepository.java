package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.ProductImage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductImageRepository extends Repository<ProductImage, Long> {

    @Query("""
        select i
        from ProductImage i
        join fetch i.variant v
        where v.id in :variantIds
        order by v.id,
            case when i.primary = true then 0 else 1 end,
            i.sortOrder,
            i.id
        """)
    List<ProductImage> findOrderedByVariantIds(@Param("variantIds") Collection<Long> variantIds);
}
