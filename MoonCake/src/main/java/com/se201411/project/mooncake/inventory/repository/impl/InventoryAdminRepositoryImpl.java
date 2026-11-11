package com.se201411.project.mooncake.inventory.repository.impl;

import java.time.LocalDate;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class InventoryAdminRepositoryImpl implements InventoryAdminRepository {

    private static final int PAGE_SIZE = 20;

    private final EntityManager em;

    public InventoryAdminRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<InventoryBalance> balances() {
        String query = """
            from InventoryBalance b
            join fetch b.variant v
            join fetch v.product
            order by v.sku
            """;
        return em.createQuery(query, InventoryBalance.class)
            .getResultList();
    }

    @Override
    public List<StockMovement> movements(int page) {
        String query = """
            from StockMovement m
            join fetch m.variant v
            join fetch v.product
            order by m.id desc
            """;
        return em.createQuery(query, StockMovement.class)
            .setFirstResult(page * PAGE_SIZE)
            .setMaxResults(PAGE_SIZE)
            .getResultList();
    }

    @Override
    public boolean isGiftSet(long id) {
        ProductVariant variant = em.find(ProductVariant.class, id);
        if (variant == null) {
            return false;
        }
        return variant.getProduct().getProductType() == ProductType.GIFT_SET;
    }

    @Override
    public boolean hasStockOrHistory(long id) {
        String stockQuery = "select count(b) from InventoryBalance b where b.variantId = :id and b.availableQuantity > 0";
        Long stock = em.createQuery(stockQuery, Long.class)
            .setParameter("id", id)
            .getSingleResult();
        if (stock > 0) {
            return true;
        }
        Long history = em.createQuery("select count(m) from StockMovement m where m.variant.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
        return history > 0;
    }

    @Override
    public void lockOperation(String group) {
        String sql = """
            SET NOCOUNT ON;
            DECLARE @result int;
            EXEC @result = sys.sp_getapplock @Resource = :key, @LockMode = 'Exclusive',
                @LockOwner = 'Transaction', @LockTimeout = 10000;
            SELECT @result;
            """;
        Number result = (Number) em.createNativeQuery(sql)
            .setParameter("key", "MoonCake:" + group)
            .getSingleResult();
        if (result.intValue() < 0) {
            throw new ConcurrentOperationException("Thao tác kho đang được xử lý. Hãy tải lại trước khi thử lại.");
        }
    }

    @Override
    public boolean operationExists(String group) {
        Long count = em.createQuery("select count(m) from StockMovement m where m.operationGroupKey = :key", Long.class)
            .setParameter("key", group)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public int change(long id, int delta) {
        String sql = """
            UPDATE dbo.INVENTORY_BALANCE
            SET available_quantity = available_quantity + :delta,
                stock_version = stock_version + 1,
                updated_at = SYSUTCDATETIME()
            WHERE variant_id = :id
              AND CAST(available_quantity AS BIGINT) + :delta BETWEEN 0 AND 2147483647
            """;
        return em.createNativeQuery(sql)
            .setParameter("id", id)
            .setParameter("delta", delta)
            .executeUpdate();
    }

    @Override
    public void movement(
        long variant,
        long admin,
        int delta,
        StockMovementType type,
        String reason,
        String group,
        String key,
        LocalDateTime now
    ) {
        StockMovement movement = new StockMovement();
        movement.setVariant(em.getReference(ProductVariant.class, variant));
        movement.setPerformedByAccount(em.getReference(Account.class, admin));
        movement.setQuantityDelta(delta);
        movement.setMovementType(type);
        movement.setReason(reason);
        movement.setOperationGroupKey(group);
        movement.setOperationKey(key);
        movement.setCreatedAt(now);
        em.persist(movement);
        em.flush();
    }

    @Override
    public long countLowStockVariants() {
        String query = """
            select count(balance)
            from InventoryBalance balance
            where balance.availableQuantity <= balance.lowStockThreshold
              and balance.variant.active = true
            """;
        return em.createQuery(query, Long.class)
            .getSingleResult();
    }

    @Override
    public long countBatchesExpiringBy(LocalDate until) {
        String query = """
            select count(batch)
            from StockBatch batch
            where batch.remainingQuantity > 0
              and batch.expiresOn is not null
              and batch.expiresOn <= :until
            """;
        return em.createQuery(query, Long.class)
            .setParameter("until", until)
            .getSingleResult();
    }
}
