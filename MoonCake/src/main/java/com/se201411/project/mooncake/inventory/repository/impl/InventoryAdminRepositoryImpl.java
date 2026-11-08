package com.se201411.project.mooncake.inventory.repository.impl;

import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;

import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.StockMovementType;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class InventoryAdminRepositoryImpl implements InventoryAdminRepository {
    private final EntityManager em;

    public InventoryAdminRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<InventoryBalance> balances() {
        return em.createQuery("from InventoryBalance b join fetch b.variant v join fetch v.product order by v.sku", InventoryBalance.class).getResultList();
    }

    @Override
    public List<StockMovement> movements(int page) {
        return em.createQuery("from StockMovement m join fetch m.variant order by m.id desc", StockMovement.class)
                .setFirstResult(page * 50).setMaxResults(50).getResultList();
    }

    @Override
    public boolean isGiftSet(long id) {
        ProductVariant v = em.find(ProductVariant.class, id);
        return v != null && v.getProduct().getProductType() == com.se201411.project.mooncake.enums.ProductType.GIFT_SET;
    }

    @Override
    public boolean hasStockOrHistory(long id) {
        return em.createQuery("select count(b) from InventoryBalance b where b.variantId=:id and b.availableQuantity>0", Long.class).setParameter("id", id).getSingleResult() > 0
                || em.createQuery("select count(m) from StockMovement m where m.variant.id=:id", Long.class).setParameter("id", id).getSingleResult() > 0;
    }

    @Override
    public void lockOperation(String group) {
        Number result = (Number) em.createNativeQuery("""
                SET NOCOUNT ON;
                DECLARE @result int;
                EXEC @result = sys.sp_getapplock @Resource=:key, @LockMode='Exclusive',
                    @LockOwner='Transaction', @LockTimeout=10000;
                SELECT @result;
                """).setParameter("key", "MoonCake:" + group).getSingleResult();
        if (result.intValue() < 0)
            throw new com.se201411.project.mooncake.common.exception.ConcurrentOperationException(
                    "Thao tác kho đang được xử lý. Hãy tải lại trước khi thử lại.");
    }

    @Override
    public boolean operationExists(String group) {
        return em.createQuery("select count(m) from StockMovement m where m.operationGroupKey=:key", Long.class).setParameter("key", group).getSingleResult() > 0;
    }

    @Override
    public int change(long id, int delta) {
        return em.createNativeQuery("""
                UPDATE dbo.INVENTORY_BALANCE SET available_quantity=available_quantity+:delta,
                stock_version=stock_version+1, updated_at=SYSUTCDATETIME()
                WHERE variant_id=:id AND CAST(available_quantity AS BIGINT)+:delta BETWEEN 0 AND 2147483647
                """).setParameter("id", id).setParameter("delta", delta).executeUpdate();
    }

    @Override
    public void movement(long variant, long admin, int delta, StockMovementType type,
                         String reason, String group, String key, LocalDateTime now) {
        StockMovement m = new StockMovement();
        m.setVariant(em.getReference(ProductVariant.class, variant));
        m.setPerformedByAccount(em.getReference(Account.class, admin));
        m.setQuantityDelta(delta);
        m.setMovementType(type);
        m.setReason(reason);
        m.setOperationGroupKey(group);
        m.setOperationKey(key);
        m.setCreatedAt(now);
        em.persist(m);
        em.flush();
    }
}
