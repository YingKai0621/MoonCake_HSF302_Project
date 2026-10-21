package com.se201411.project.mooncake.inventory.repository;

import com.se201411.project.mooncake.entity.InventoryBalance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository
    extends Repository<InventoryBalance, Long> {

    boolean existsById(Long variantId);

    @Modifying
    @Query(value = """
        INSERT INTO dbo.INVENTORY_BALANCE
            (variant_id, available_quantity, low_stock_threshold, stock_version, updated_at)
        SELECT :variantId, 0, 5, 0, SYSUTCDATETIME()
        WHERE EXISTS (
            SELECT 1 FROM dbo.PRODUCT_VARIANT WHERE variant_id = :variantId
        )
          AND NOT EXISTS (
            SELECT 1 FROM dbo.INVENTORY_BALANCE WHERE variant_id = :variantId
        )
        """, nativeQuery = true)
    int initializeIfMissing(@Param("variantId") long variantId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE dbo.INVENTORY_BALANCE
        SET available_quantity = available_quantity - :quantity,
            stock_version = stock_version + 1,
            updated_at = SYSUTCDATETIME()
        WHERE variant_id = :variantId
          AND available_quantity >= :quantity
        """, nativeQuery = true)
    int deductAvailable(
        @Param("variantId") long variantId,
        @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE dbo.INVENTORY_BALANCE
        SET available_quantity = available_quantity + :quantity,
            stock_version = stock_version + 1,
            updated_at = SYSUTCDATETIME()
        WHERE variant_id = :variantId
        """, nativeQuery = true)
    int restoreAvailable(
        @Param("variantId") long variantId,
        @Param("quantity") int quantity
    );
}
