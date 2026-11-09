package com.se201411.project.mooncake.order.dto;

public record AutoCancelView(
    String formattedTime,
    long deadlineEpochMillis,
    long startEpochMillis
) {
}
