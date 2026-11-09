package com.se201411.project.mooncake.feedback.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Feedback;
import com.se201411.project.mooncake.feedback.dto.AdminFeedbackView;
import com.se201411.project.mooncake.feedback.dto.FeedbackForm;
import com.se201411.project.mooncake.feedback.dto.PublicReviewView;
import com.se201411.project.mooncake.feedback.repository.FeedbackRepository;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int HOMEPAGE_MIN_RATING = 4;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FeedbackRepository feedbackRepository;
    private final CurrentCustomerService currentCustomerService;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public FeedbackServiceImpl(
        FeedbackRepository feedbackRepository,
        CurrentCustomerService currentCustomerService,
        AdminAccessService adminAccessService,
        Clock clock
    ) {
        this.feedbackRepository = feedbackRepository;
        this.currentCustomerService = currentCustomerService;
        this.adminAccessService = adminAccessService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void submit(FeedbackForm form) {
        Integer rating = form.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessRuleViolationException("Vui lòng chọn từ 1 đến 5 sao.");
        }
        String content = "";
        if (form.getContent() != null) {
            content = form.getContent().trim();
        }
        if (content.isEmpty() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessRuleViolationException("Góp ý cần từ 1 đến 2.000 ký tự.");
        }

        Feedback feedback = new Feedback();
        feedback.setCustomerId(currentCustomerService.requireCurrentCustomer().customerId());
        feedback.setRating(rating);
        feedback.setContent(content);
        feedback.setPublished(false);
        feedback.setCreatedAt(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)));
        feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicReviewView> findPublishedReviews() {
        List<PublicReviewView> reviews = new ArrayList<>();
        List<Feedback> publishedReviews =
            feedbackRepository.findTop20ByPublishedTrueAndRatingGreaterThanEqualOrderByCreatedAtDesc(HOMEPAGE_MIN_RATING);
        for (Feedback feedback : publishedReviews) {
            String customerName = feedback.getCustomer().getFullName();
            reviews.add(new PublicReviewView(
                customerName,
                initialOf(customerName),
                feedback.getRating(),
                feedback.getContent()
            ));
        }
        return reviews;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminFeedbackView> findForAdmin() {
        adminAccessService.requireAdmin();
        List<AdminFeedbackView> views = new ArrayList<>();
        for (Feedback feedback : feedbackRepository.findTop100ByOrderByIdDesc()) {
            views.add(new AdminFeedbackView(
                feedback.getId(),
                feedback.getCustomer().getFullName(),
                feedback.getRating(),
                feedback.getContent(),
                formatVietnamTime(feedback.getCreatedAt()),
                feedback.isPublished()
            ));
        }
        return views;
    }

    @Override
    @Transactional
    public void publish(long feedbackId) {
        adminAccessService.requireAdmin();
        Feedback feedback = requireFeedback(feedbackId);
        if (feedback.getRating() == null) {
            throw new BusinessRuleViolationException("Chỉ hiển thị được góp ý có chấm sao.");
        }
        feedback.setPublished(true);
    }

    @Override
    @Transactional
    public void hide(long feedbackId) {
        adminAccessService.requireAdmin();
        Feedback feedback = requireFeedback(feedbackId);
        feedback.setPublished(false);
    }

    private Feedback requireFeedback(long feedbackId) {
        return feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy góp ý."));
    }

    private String initialOf(String fullName) {
        String name = fullName.trim();
        if (name.isEmpty()) {
            return "?";
        }
        String lastWord = name.substring(name.lastIndexOf(' ') + 1);
        return lastWord.substring(0, 1).toUpperCase();
    }

    private String formatVietnamTime(LocalDateTime utcTime) {
        return utcTime.atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(VIETNAM)
            .format(DATE_TIME);
    }
}
