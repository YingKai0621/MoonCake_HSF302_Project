package com.se201411.project.mooncake.dashboard.dto;

public record KpiView(
    String label,
    long value,
    boolean money,
    Double changePercent,
    String compareLabel,
    long previousValue,
    String previousLabel
) {
    public String trend() {
        if (value == 0 && previousValue > 0) {
            return "waiting";
        }
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
