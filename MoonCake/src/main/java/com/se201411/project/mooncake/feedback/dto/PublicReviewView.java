package com.se201411.project.mooncake.feedback.dto;

public record PublicReviewView(
    String customerName,
    int rating,
    String content
) {
}
