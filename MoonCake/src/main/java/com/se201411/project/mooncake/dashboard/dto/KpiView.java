package com.se201411.project.mooncake.dashboard.dto;

public record KpiView(
    String label,
    long value,
    boolean money,
    Double changePercent,
    String compareLabel
) {
    public String trend() {
        if (changePercent == null) {
            return value > 0 ? "new" : "flat";
        }
        if (changePercent > 0) {
            return "up";
        }
        if (changePercent < 0) {
            return "down";
        }
        return "flat";
    }
}
