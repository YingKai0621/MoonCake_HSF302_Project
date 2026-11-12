package com.se201411.project.mooncake.feedback.dto;

import com.se201411.project.mooncake.entity.Feedback;

public record PublicReviewView(
    String customerName,
    String customerInitial,
    int rating,
    String content
) {

    public static PublicReviewView entityToDto(Feedback feedback) {
        String customerName = feedback.getCustomer().getFullName();
        return new PublicReviewView(
            customerName,
            initialOf(customerName),
            feedback.getRating(),
            feedback.getContent()
        );
    }

    private static String initialOf(String fullName) {
        String name = fullName.trim();
        if (name.isEmpty()) {
            return "?";
        }
        String lastWord = name.substring(name.lastIndexOf(' ') + 1);
        return lastWord.substring(0, 1).toUpperCase();
    }
}
