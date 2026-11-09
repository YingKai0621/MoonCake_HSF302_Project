package com.se201411.project.mooncake.dashboard.dto;

public enum DashboardPeriod {
    DAY("Ngày", "so với hôm qua", 14),
    MONTH("Tháng", "so với tháng trước", 12),
    QUARTER("Quý", "so với quý trước", 8),
    YEAR("Năm", "so với năm trước", 5);

    private final String label;
    private final String compareLabel;
    private final int chartBuckets;

    DashboardPeriod(String label, String compareLabel, int chartBuckets) {
        this.label = label;
        this.compareLabel = compareLabel;
        this.chartBuckets = chartBuckets;
    }

    public String getLabel() {
        return label;
    }

    public String getCompareLabel() {
        return compareLabel;
    }

    public int getChartBuckets() {
        return chartBuckets;
    }
}
