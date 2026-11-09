package com.se201411.project.mooncake.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.dto.BusinessSummaryView;
import com.se201411.project.mooncake.order.dto.OverviewStatsView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminOverviewRepository extends JpaRepository<SalesOrder, Long> {

    @Query("""
        select new com.se201411.project.mooncake.order.dto.OverviewStatsView(
            count(salesOrder),
            coalesce(sum(case when salesOrder.status = :placed then 1 else 0 end), 0),
            coalesce(sum(case when salesOrder.status = :completed then salesOrder.totalAmount else 0 end), 0),
            coalesce(sum(case when salesOrder.status = :cancelled then 1 else 0 end), 0)
        )
        from SalesOrder salesOrder
        """)
    OverviewStatsView statistics(
        @Param("placed") OrderStatus placed,
        @Param("completed") OrderStatus completed,
        @Param("cancelled") OrderStatus cancelled
    );

    @Query("""
        select new com.se201411.project.mooncake.order.dto.BusinessSummaryView(
            business.id,
            business.name,
            business.contactName,
            business.contactEmail,
            business.contactPhone,
            count(salesOrder.id),
            coalesce(sum(salesOrder.totalAmount), 0)
        )
        from Business business
        left join SalesOrder salesOrder on salesOrder.business = business
        group by business.id, business.name, business.contactName,
            business.contactEmail, business.contactPhone
        order by business.id desc
        """)
    List<BusinessSummaryView> businessSummaries();
}
