package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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

    public SalesOrder requireForUpdate(long id) {
        SalesOrder order = em.find(SalesOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng.");
        }
        return order;
    }

    public int transition(long id, OrderStatus from, OrderStatus to, LocalDateTime now) {
        int changed = em.createQuery("""
                update SalesOrder o
                set o.status = :to, o.updatedAt = :now
                where o.id = :id and o.status = :from
                """)
                .setParameter("id", id)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("now", now)
                .executeUpdate();
        em.clear();
        return changed;
    }
}
