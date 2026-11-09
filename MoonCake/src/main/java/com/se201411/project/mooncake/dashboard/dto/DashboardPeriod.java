package com.se201411.project.mooncake.dashboard.dto;

public enum DashboardPeriod {
    DAY("Ngày", "so với hôm qua", "Hôm qua", 14),
    MONTH("Tháng", "so với tháng trước", "Tháng trước", 12),
    QUARTER("Quý", "so với quý trước", "Quý trước", 8),
    YEAR("Năm", "so với năm trước", "Năm trước", 5);

    private final String label;
    private final String compareLabel;
    private final String previousLabel;
    private final int chartBuckets;

    DashboardPeriod(String label, String compareLabel, String previousLabel, int chartBuckets) {
        this.label = label;
        this.compareLabel = compareLabel;
        this.previousLabel = previousLabel;
        this.chartBuckets = chartBuckets;
    }

    public String getPreviousLabel() {
        return previousLabel;
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
