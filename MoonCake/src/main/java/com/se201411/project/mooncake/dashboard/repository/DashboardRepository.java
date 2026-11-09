package com.se201411.project.mooncake.dashboard.repository;

import com.se201411.project.mooncake.dashboard.dto.PaidAmount;
import com.se201411.project.mooncake.dashboard.dto.TopProductView;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DashboardRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByCreatedAtGreaterThanEqual(LocalDateTime from);

    List<SalesOrder> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    @Query("""
        select new com.se201411.project.mooncake.dashboard.dto.PaidAmount(payment.amount, payment.paidAt)
        from Payment payment
        where payment.status = :paid
          and payment.paidAt >= :from
        """)
    List<PaidAmount> findPaidSince(
        @Param("paid") PaymentStatus paid,
        @Param("from") LocalDateTime from
    );

    @Query("""
        select count(payment)
        from Payment payment
        where payment.order.status = :cancelled
          and payment.status = :paid
        """)
    long countPaidCancelledOrders(
        @Param("cancelled") OrderStatus cancelled,
        @Param("paid") PaymentStatus paid
    );

    @Query("""
        select count(balance)
        from InventoryBalance balance
        where balance.availableQuantity <= balance.lowStockThreshold
          and balance.variant.active = true
        """)
    long countLowStockVariants();

    @Query("""
        select count(batch)
        from StockBatch batch
        where batch.remainingQuantity > 0
          and batch.expiresOn is not null
          and batch.expiresOn <= :until
        """)
    long countBatchesExpiringBy(@Param("until") LocalDate until);

    @Query("""
        select count(feedback)
        from Feedback feedback
        where feedback.published = false
          and feedback.rating is not null
        """)
    long countReviewsWaitingForApproval();

    @Query("""
        select new com.se201411.project.mooncake.dashboard.dto.TopProductView(
            item.productNameSnapshot, sum(item.quantity), sum(item.lineTotal)
        )
        from OrderItem item
        where item.order.createdAt >= :from
          and item.order.status <> :cancelled
        group by item.productNameSnapshot
        order by sum(item.quantity) desc
        """)
    List<TopProductView> findTopProducts(
        @Param("from") LocalDateTime from,
        @Param("cancelled") OrderStatus cancelled,
        Pageable pageable
    );
}
