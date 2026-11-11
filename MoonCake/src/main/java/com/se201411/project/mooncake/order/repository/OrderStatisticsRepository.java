package com.se201411.project.mooncake.order.repository;

import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.PaidAmount;
import com.se201411.project.mooncake.order.dto.ProductSalesSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderStatisticsRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByCreatedAtGreaterThanEqual(LocalDateTime from);

    List<SalesOrder> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    List<SalesOrder> findTop5ByDiscountAmountGreaterThanAndStatusNotOrderByCreatedAtDesc(
        BigDecimal discount,
        OrderStatus status
    );

    @Query("""
        select new com.se201411.project.mooncake.order.dto.PaidAmount(payment.amount, payment.paidAt)
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
        select new com.se201411.project.mooncake.order.dto.ProductSalesSummary(
            item.productNameSnapshot, sum(item.quantity), sum(item.lineTotal)
        )
        from OrderItem item
        where item.order.createdAt >= :from
          and item.order.status in :statuses
        group by item.productNameSnapshot
        order by sum(item.quantity) desc
        """)
    List<ProductSalesSummary> findTopProducts(
        @Param("from") LocalDateTime from,
        @Param("statuses") Collection<OrderStatus> statuses,
        Pageable pageable
    );
}
