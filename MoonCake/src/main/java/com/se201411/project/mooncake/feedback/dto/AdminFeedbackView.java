package com.se201411.project.mooncake.feedback.dto;

public record AdminFeedbackView(
    long id,
    String customerName,
    Integer rating,
    String content,
    String formattedCreatedAt,
    boolean published
) {
}
