package com.se201411.project.mooncake.dashboard.dto;

import java.util.List;

public record DashboardView(
    DashboardPeriod period,
    List<KpiView> kpis,
    List<String> chartLabels,
    List<Long> chartRevenue,
    List<Long> chartOrders,
    List<DashboardTaskView> tasks,
    List<RecentOrderView> recentOrders,
    List<TopProductView> topProducts
) {
}
