package com.se201411.project.mooncake.order.repository.impl;

import java.time.LocalDate;
import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.order.repository.OrderAdminRepository;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderAdminRepositoryImpl implements OrderAdminRepository {

    private static final int PAGE_SIZE = 50;

    private final EntityManager em;

    public OrderAdminRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<SalesOrder> orders(OrderStatus status, int page) {
        String query = "from SalesOrder o where (:status is null or o.status = :status) order by o.id desc";
        return em.createQuery(query, SalesOrder.class)
            .setParameter("status", status)
            .setFirstResult(page * PAGE_SIZE)
            .setMaxResults(PAGE_SIZE)
            .getResultList();
    }

    @Override
    public long count(OrderStatus status) {
        return em.createQuery("select count(o) from SalesOrder o where (:status is null or o.status = :status)", Long.class)
            .setParameter("status", status)
            .getSingleResult();
    }

    @Override
    public SalesOrder require(long id) {
        SalesOrder order = em.find(SalesOrder.class, id);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng.");
        }
        return order;
    }

    @Override
    public SalesOrder requireForUpdate(long id) {
        SalesOrder order = em.find(SalesOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng.");
        }
        return order;
    }

    @Override
    public int transition(long id, OrderStatus from, OrderStatus to, LocalDateTime now) {
        String query = "update SalesOrder o set o.status = :to, o.updatedAt = :now, o."
            + timestampField(to)
            + " = :now where o.id = :id and o.status = :from";
        int changed = em.createQuery(query)
                .setParameter("id", id)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("now", now)
                .executeUpdate();
        em.clear();
        return changed;
    }

    @Override
    public int ship(long id, ShippingCarrier carrier, String trackingCode, LocalDate estimatedDeliveryDate, LocalDateTime now) {
        int changed = em.createQuery("""
                update SalesOrder o
                set o.status = :shipping,
                    o.updatedAt = :now,
                    o.shippingAt = :now,
                    o.shippingCarrier = :carrier,
                    o.trackingCode = :trackingCode,
                    o.estimatedDeliveryDate = :estimatedDeliveryDate
                where o.id = :id and o.status = :processing
                """)
                .setParameter("id", id)
                .setParameter("shipping", OrderStatus.SHIPPING)
                .setParameter("processing", OrderStatus.PROCESSING)
                .setParameter("now", now)
                .setParameter("carrier", carrier)
                .setParameter("trackingCode", trackingCode)
                .setParameter("estimatedDeliveryDate", estimatedDeliveryDate)
                .executeUpdate();
        em.clear();
        return changed;
    }

    private String timestampField(OrderStatus status) {
        if (status == OrderStatus.PROCESSING) {
            return "processingAt";
        }
        if (status == OrderStatus.DELIVERED) {
            return "deliveredAt";
        }
        throw new IllegalArgumentException("Trạng thái không có mốc thời gian riêng: " + status);
    }
}
