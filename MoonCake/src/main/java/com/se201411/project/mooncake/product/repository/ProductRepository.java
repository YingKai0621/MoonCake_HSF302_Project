package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.enums.ProductType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends Repository<Product, Long> {

    @Query("""
        select p
        from Product p
        join fetch p.category c
        where p.active = true
          and c.active = true
          and p.productType = :productType
        order by p.name, p.id
        """)
    List<Product> findActiveByType(@Param("productType") ProductType productType);

    @Query("""
        select p
        from Product p
        join fetch p.category c
        where p.active = true
          and c.active = true
          and p.slug = :slug
        """)
    Optional<Product> findActiveBySlug(@Param("slug") String slug);
}
