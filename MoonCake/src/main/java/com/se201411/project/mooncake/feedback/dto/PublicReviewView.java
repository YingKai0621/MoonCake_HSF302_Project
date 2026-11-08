package com.se201411.project.mooncake.feedback.dto;

public record PublicReviewView(
    String customerName,
    String customerInitial,
    int rating,
    String content
) {
}
