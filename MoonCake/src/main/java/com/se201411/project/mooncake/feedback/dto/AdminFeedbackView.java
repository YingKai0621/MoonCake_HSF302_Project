package com.se201411.project.mooncake.feedback.dto;

import com.se201411.project.mooncake.entity.Feedback;

public record AdminFeedbackView(
    long id,
    String customerName,
    Integer rating,
    String content,
    String formattedCreatedAt,
    boolean published
) {

    public static AdminFeedbackView entityToDto(Feedback feedback, String formattedCreatedAt) {
        return new AdminFeedbackView(
            feedback.getId(),
            feedback.getCustomer().getFullName(),
            feedback.getRating(),
            feedback.getContent(),
            formattedCreatedAt,
            feedback.isPublished()
        );
    }
}
