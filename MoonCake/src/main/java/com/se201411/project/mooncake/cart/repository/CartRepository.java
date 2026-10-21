package com.se201411.project.mooncake.cart.repository;

import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.enums.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends Repository<Cart, Long> {

    @Query("""
        select c
        from Cart c
        where c.customer.id = :customerId
          and c.status = :status
        """)
    Optional<Cart> findByCustomerIdAndStatus(
        @Param("customerId") long customerId,
        @Param("status") CartStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select c
        from Cart c
        where c.customer.id = :customerId
          and c.status = :status
        """)
    Optional<Cart> findByCustomerIdAndStatusForUpdate(
        @Param("customerId") long customerId,
        @Param("status") CartStatus status
    );

    @Query("""
        select c
        from Cart c
        where c.id = :cartId
          and c.customer.id = :customerId
          and c.status = :status
        """)
    Optional<Cart> findOwnedCart(
        @Param("cartId") long cartId,
        @Param("customerId") long customerId,
        @Param("status") CartStatus status
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE dbo.CART
        SET cart_status = 'CHECKED_OUT',
            updated_at = SYSUTCDATETIME()
        WHERE cart_id = :cartId
          AND customer_id = :customerId
          AND cart_status = 'ACTIVE'
        """, nativeQuery = true)
    int claimForCheckout(
        @Param("cartId") long cartId,
        @Param("customerId") long customerId
    );

    /**
     * SQL Server range locking prevents two first-add requests from creating two
     * ACTIVE carts for the same customer.
     */
    @Modifying
    @Query(value = """
        INSERT INTO dbo.CART
            (customer_id, guest_token, cart_status, created_at, updated_at)
        SELECT
            :customerId, NULL, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME()
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.CART WITH (UPDLOCK, HOLDLOCK)
            WHERE customer_id = :customerId
              AND cart_status = 'ACTIVE'
        )
        """, nativeQuery = true)
    int createActiveCartIfMissing(@Param("customerId") long customerId);
}
