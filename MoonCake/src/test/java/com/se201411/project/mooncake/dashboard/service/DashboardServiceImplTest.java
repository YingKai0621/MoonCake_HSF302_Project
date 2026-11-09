package com.se201411.project.mooncake.dashboard.service;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.dto.DashboardView;
import com.se201411.project.mooncake.dashboard.dto.KpiView;
import com.se201411.project.mooncake.dashboard.dto.PaidAmount;
import com.se201411.project.mooncake.dashboard.repository.DashboardRepository;
import com.se201411.project.mooncake.dashboard.service.impl.DashboardServiceImpl;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceImplTest {

    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final DashboardService service = new DashboardServiceImpl(
        repository,
        mock(AdminAccessService.class),
        Clock.fixed(Instant.parse("2026-11-09T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void dayViewComparesTodayWithYesterdayInVietnamTime() {
        when(repository.findByCreatedAtGreaterThanEqual(any())).thenReturn(List.of(
            order(OrderStatus.COMPLETED, 300_000, LocalDateTime.of(2026, 11, 9, 1, 0)),
            order(OrderStatus.PLACED, 100_000, LocalDateTime.of(2026, 11, 8, 18, 0)),
            order(OrderStatus.COMPLETED, 200_000, LocalDateTime.of(2026, 11, 8, 2, 0)),
            order(OrderStatus.CANCELLED, 999_000, LocalDateTime.of(2026, 11, 9, 2, 0))
        ));
        when(repository.findPaidSince(eq(PaymentStatus.PAID), any())).thenReturn(List.of(
            new PaidAmount(new BigDecimal("300000"), LocalDateTime.of(2026, 11, 9, 1, 0))
        ));

        DashboardView view = service.dashboard(DashboardPeriod.DAY);

        KpiView revenue = view.kpis().get(0);
        KpiView orders = view.kpis().get(1);
        assertThat(view.chartLabels()).hasSize(14).endsWith("09/11");
        assertThat(revenue.value()).isEqualTo(300_000);
        assertThat(revenue.changePercent()).isEqualTo(50.0);
        assertThat(orders.value()).isEqualTo(2);
        assertThat(orders.changePercent()).isEqualTo(100.0);
        assertThat(view.kpis().get(3).value()).isEqualTo(300_000);
    }

    @Test
    void quarterViewUsesEightQuarters() {
        when(repository.findByCreatedAtGreaterThanEqual(any())).thenReturn(List.of());
        when(repository.findPaidSince(eq(PaymentStatus.PAID), any())).thenReturn(List.of());

        DashboardView view = service.dashboard(DashboardPeriod.QUARTER);

        assertThat(view.chartLabels()).hasSize(8).endsWith("Q4/2026");
        assertThat(view.kpis().get(0).trend()).isEqualTo("flat");
    }

    private SalesOrder order(OrderStatus status, long total, LocalDateTime createdAtUtc) {
        SalesOrder order = new SalesOrder();
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(total));
        order.setCreatedAt(createdAtUtc);
        return order;
    }
}
