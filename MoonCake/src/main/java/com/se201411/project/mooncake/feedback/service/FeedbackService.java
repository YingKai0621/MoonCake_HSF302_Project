package com.se201411.project.mooncake.feedback.service;

import com.se201411.project.mooncake.feedback.dto.AdminFeedbackView;
import com.se201411.project.mooncake.feedback.dto.FeedbackForm;
import com.se201411.project.mooncake.feedback.dto.PublicReviewView;

import java.util.List;

public interface FeedbackService {

    void submit(FeedbackForm form);

    List<PublicReviewView> findPublishedReviews();

    List<AdminFeedbackView> findForAdmin();

    void publish(long feedbackId);

    void hide(long feedbackId);
}
