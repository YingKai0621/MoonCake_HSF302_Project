package com.se201411.project.mooncake.dashboard.dto;

public record DashboardTaskView(
    String label,
    long count,
    String url,
    String tone
) {
}
