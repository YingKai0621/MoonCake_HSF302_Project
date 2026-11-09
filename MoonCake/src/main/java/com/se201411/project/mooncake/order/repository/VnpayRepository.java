package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.entity.VnpayAttempt;
import com.se201411.project.mooncake.order.dto.VnpayAttemptView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VnpayRepository extends JpaRepository<VnpayAttempt, String> {

    long ATTEMPT_ROW_LIFETIME_MINUTES = 15;

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select salesOrder from SalesOrder salesOrder where salesOrder.id = :orderId")
    Optional<SalesOrder> findOrderForUpdate(@Param("orderId") long orderId);

    @Query("""
        select new com.se201411.project.mooncake.order.dto.VnpayAttemptView(
            attempt.transactionRef, attempt.orderId, attempt.amount, attempt.status, attempt.createdAt
        )
        from VnpayAttempt attempt
        where attempt.transactionRef = :transactionRef
        """)
    Optional<VnpayAttemptView> findView(@Param("transactionRef") String transactionRef);

    @Query("""
        select new com.se201411.project.mooncake.order.dto.VnpayAttemptView(
            attempt.transactionRef, attempt.orderId, attempt.amount, attempt.status, attempt.createdAt
        )
        from VnpayAttempt attempt
        where attempt.orderId = :orderId
          and attempt.status = 'PENDING'
        order by attempt.createdAt desc
        """)
    List<VnpayAttemptView> findPendingViews(@Param("orderId") long orderId);

    @Modifying(flushAutomatically = true)
    @Query("""
        update VnpayAttempt attempt
        set attempt.status = 'FAILED'
        where attempt.transactionRef = :transactionRef
          and attempt.status = 'PENDING'
        """)
    int markFailed(@Param("transactionRef") String transactionRef);

    @Modifying(flushAutomatically = true)
    @Query("""
        update VnpayAttempt attempt
        set attempt.status = 'PAID',
            attempt.transactionNo = :transactionNo
        where attempt.transactionRef = :transactionRef
          and attempt.status in ('PENDING', 'FAILED')
        """)
    int markPaid(
        @Param("transactionRef") String transactionRef,
        @Param("transactionNo") String transactionNo
    );

    default SalesOrder lockOrder(long orderId) {
        return findOrderForUpdate(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
    }

    default VnpayAttemptView findAttempt(String transactionRef) {
        return findView(transactionRef).orElse(null);
    }

    default VnpayAttemptView findPendingAttempt(long orderId) {
        List<VnpayAttemptView> pending = findPendingViews(orderId);
        if (pending.isEmpty()) {
            return null;
        }
        return pending.get(0);
    }

    default void createAttempt(
        String transactionRef,
        long orderId,
        BigDecimal amount,
        LocalDateTime createdAt
    ) {
        VnpayAttempt attempt = new VnpayAttempt();
        attempt.setTransactionRef(transactionRef);
        attempt.setOrderId(orderId);
        attempt.setAmount(amount);
        attempt.setCreatedAt(createdAt);
        attempt.setExpiresAt(createdAt.plusMinutes(ATTEMPT_ROW_LIFETIME_MINUTES));
        attempt.setStatus("PENDING");
        save(attempt);
    }
}
