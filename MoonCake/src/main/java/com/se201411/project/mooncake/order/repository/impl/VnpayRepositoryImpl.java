package com.se201411.project.mooncake.order.repository.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class VnpayRepositoryImpl implements VnpayRepository {
    private static final String SELECT_ATTEMPT =
        "SELECT txn_ref, order_id, amount, status, created_at FROM dbo.VNPAY_ATTEMPT ";

    private final EntityManager entityManager;

    public VnpayRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SalesOrder lockOrder(long orderId) {
        SalesOrder order = entityManager.find(
            SalesOrder.class,
            orderId,
            LockModeType.PESSIMISTIC_WRITE
        );
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng.");
        }
        return order;
    }

    @Override
    public Attempt findAttempt(String transactionRef) {
        List<?> rows = entityManager.createNativeQuery(SELECT_ATTEMPT + "WHERE txn_ref = :ref")
            .setParameter("ref", transactionRef)
            .getResultList();
        return firstAttempt(rows);
    }

    @Override
    public Attempt findPendingAttempt(long orderId) {
        List<?> rows = entityManager.createNativeQuery(
                SELECT_ATTEMPT + "WHERE order_id = :orderId AND status = 'PENDING' "
                    + "ORDER BY created_at DESC"
            )
            .setParameter("orderId", orderId)
            .setMaxResults(1)
            .getResultList();
        return firstAttempt(rows);
    }

    @Override
    public void createAttempt(
        String transactionRef,
        long orderId,
        BigDecimal amount,
        LocalDateTime createdAt
    ) {
        entityManager.createNativeQuery("""
                INSERT dbo.VNPAY_ATTEMPT(txn_ref, order_id, amount, created_at, expires_at)
                VALUES(:ref, :orderId, :amount, :createdAt, :expiresAt)
                """)
            .setParameter("ref", transactionRef)
            .setParameter("orderId", orderId)
            .setParameter("amount", amount)
            .setParameter("createdAt", createdAt)
            .setParameter("expiresAt", createdAt.plusMinutes(15))
            .executeUpdate();
    }

    @Override
    public void markFailed(String transactionRef) {
        entityManager.createNativeQuery("""
                UPDATE dbo.VNPAY_ATTEMPT
                SET status = 'FAILED'
                WHERE txn_ref = :ref AND status = 'PENDING'
                """)
            .setParameter("ref", transactionRef)
            .executeUpdate();
    }

    @Override
    public void markPaid(String transactionRef, String transactionNo) {
        entityManager.createNativeQuery("""
                UPDATE dbo.VNPAY_ATTEMPT
                SET status = 'PAID', transaction_no = :transactionNo
                WHERE txn_ref = :ref AND status IN ('PENDING', 'FAILED')
                """)
            .setParameter("ref", transactionRef)
            .setParameter("transactionNo", transactionNo)
            .executeUpdate();
    }

    private Attempt firstAttempt(List<?> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = (Object[]) rows.get(0);
        return new Attempt(
            (String) row[0],
            ((Number) row[1]).longValue(),
            (BigDecimal) row[2],
            (String) row[3],
            toLocalDateTime(row[4])
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
