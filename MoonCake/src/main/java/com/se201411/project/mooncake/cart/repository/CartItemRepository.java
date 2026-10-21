package com.se201411.project.mooncake.cart.repository;

import com.se201411.project.mooncake.entity.CartItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends Repository<CartItem, Long> {

    @Query("""
        select i
        from CartItem i
        join fetch i.variant v
        where i.cart.id = :cartId
          and i.variant is not null
        order by i.addedAt, i.id
        """)
    List<CartItem> findVariantItemsByCartId(@Param("cartId") long cartId);

    @Query("""
        select i
        from CartItem i
        join fetch i.configuration configuration
        join fetch configuration.box box
        join fetch box.emptyBoxVariant boxVariant
        join fetch boxVariant.product boxProduct
        join fetch boxProduct.category boxCategory
        where i.cart.id = :cartId
          and i.configuration is not null
        order by i.addedAt, i.id
        """)
    List<CartItem> findCustomItemsByCartId(@Param("cartId") long cartId);

    @Query("""
        select i
        from CartItem i
        join fetch i.variant v
        where i.cart.id = :cartId
          and v.id = :variantId
        """)
    Optional<CartItem> findVariantItem(
        @Param("cartId") long cartId,
        @Param("variantId") long variantId
    );

    @Query("""
        select i
        from CartItem i
        join fetch i.configuration configuration
        where i.cart.id = :cartId
          and configuration.id = :configurationId
        """)
    Optional<CartItem> findConfigurationItem(
        @Param("cartId") long cartId,
        @Param("configurationId") long configurationId
    );

    @Query("""
        select i
        from CartItem i
        left join fetch i.variant v
        where i.id = :cartItemId
          and i.cart.id = :cartId
        """)
    Optional<CartItem> findByIdAndCartId(
        @Param("cartItemId") long cartItemId,
        @Param("cartId") long cartId
    );

    @Query("""
        select coalesce(sum(i.quantity), 0)
        from CartItem i
        where i.cart.id = :cartId
        """)
    long sumQuantityByCartId(@Param("cartId") long cartId);

    CartItem save(CartItem cartItem);

    void delete(CartItem cartItem);
}
