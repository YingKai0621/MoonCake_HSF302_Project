package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderAdminRepository {
    private final EntityManager em;

    public OrderAdminRepository(EntityManager em) {
        this.em = em;
    }

    public List<SalesOrder> orders(OrderStatus status, int page) {
        return em.createQuery("from SalesOrder o where (:status is null or o.status=:status) order by o.id desc", SalesOrder.class)
                .setParameter("status", status).setFirstResult(page * 50).setMaxResults(50).getResultList();
    }

    public SalesOrder require(long id) {
        var order = em.find(SalesOrder.class, id);
        if (order == null) throw new ResourceNotFoundException("Không tìm thấy đơn hàng.");
        return order;
    }

    public int transition(long id, OrderStatus from, OrderStatus to, LocalDateTime now) {
        int changed = em.createQuery("update SalesOrder o set o.status=:to,o.updatedAt=:now where o.id=:id and o.status=:from")
                .setParameter("id", id).setParameter("from", from).setParameter("to", to).setParameter("now", now).executeUpdate();
        em.clear();
        return changed;
    }

    public int collectCod(long orderId, LocalDateTime now) {
        return em.createNativeQuery("""
                UPDATE dbo.PAYMENT SET payment_status='PAID',paid_at=:now,updated_at=:now
                WHERE order_id=:id AND payment_method='COD' AND payment_status='UNPAID'
                AND amount=(SELECT total_amount FROM dbo.SALES_ORDER WHERE order_id=:id)
                AND (SELECT COUNT(*) FROM dbo.PAYMENT WHERE order_id=:id)=1
                """).setParameter("id", orderId).setParameter("now", now).executeUpdate();
    }
}
