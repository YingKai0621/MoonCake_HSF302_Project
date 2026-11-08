package com.se201411.project.mooncake.feedback.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.Feedback;
import com.se201411.project.mooncake.feedback.dto.FeedbackForm;
import com.se201411.project.mooncake.feedback.dto.PublicReviewView;
import com.se201411.project.mooncake.feedback.repository.FeedbackRepository;
import com.se201411.project.mooncake.feedback.service.impl.FeedbackServiceImpl;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceImplTest {

    private final FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
    private final CurrentCustomerService currentCustomerService = mock(CurrentCustomerService.class);
    private final AdminAccessService adminAccessService = mock(AdminAccessService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC);
    private final FeedbackService service = new FeedbackServiceImpl(
        feedbackRepository,
        currentCustomerService,
        adminAccessService,
        clock
    );

    @Test
    void submitSavesHiddenFeedbackForCurrentCustomer() {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(new ShopperIdentity(7L));

        service.submit(form(5, "  Bánh rất ngon  "));

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getCustomerId()).isEqualTo(7L);
        assertThat(saved.getValue().getRating()).isEqualTo(5);
        assertThat(saved.getValue().getContent()).isEqualTo("Bánh rất ngon");
        assertThat(saved.getValue().isPublished()).isFalse();
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 8, 3, 0));
    }

    @Test
    void submitRejectsRatingOutsideOneToFive() {
        assertThatThrownBy(() -> service.submit(form(6, "Ngon")))
            .isInstanceOf(BusinessRuleViolationException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitRejectsBlankContent() {
        assertThatThrownBy(() -> service.submit(form(4, "   ")))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void publishedReviewsShowCustomerNameRatingAndContent() {
        Feedback feedback = feedback(5, true, "Trần Thị Lan");
        when(feedbackRepository.findTop20ByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(feedback));

        List<PublicReviewView> reviews = service.findPublishedReviews();

        assertThat(reviews).containsExactly(new PublicReviewView("Trần Thị Lan", "L", 5, "Bánh ngon"));
    }

    @Test
    void publishMakesRatedFeedbackVisible() {
        Feedback feedback = feedback(4, false, "Lan");
        when(feedbackRepository.findById(3L)).thenReturn(Optional.of(feedback));

        service.publish(3L);

        assertThat(feedback.isPublished()).isTrue();
    }

    @Test
    void publishRejectsOldFeedbackWithoutRating() {
        Feedback feedback = feedback(null, false, "Lan");
        when(feedbackRepository.findById(3L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.publish(3L))
            .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(feedback.isPublished()).isFalse();
    }

    @Test
    void hideRemovesFeedbackFromHomepage() {
        Feedback feedback = feedback(5, true, "Lan");
        when(feedbackRepository.findById(3L)).thenReturn(Optional.of(feedback));

        service.hide(3L);

        assertThat(feedback.isPublished()).isFalse();
    }

    @Test
    void onlyAdminCanPublish() {
        when(adminAccessService.requireAdmin()).thenThrow(new AccessDeniedException("no"));

        assertThatThrownBy(() -> service.publish(3L))
            .isInstanceOf(AccessDeniedException.class);
        verify(feedbackRepository, never()).findById(3L);
    }

    private FeedbackForm form(Integer rating, String content) {
        FeedbackForm form = new FeedbackForm();
        form.setRating(rating);
        form.setContent(content);
        return form;
    }

    private Feedback feedback(Integer rating, boolean published, String customerName) {
        Customer customer = mock(Customer.class);
        when(customer.getFullName()).thenReturn(customerName);
        Feedback feedback = mock(Feedback.class, CALLS_REAL_METHODS);
        feedback.setRating(rating);
        feedback.setPublished(published);
        feedback.setContent("Bánh ngon");
        when(feedback.getCustomer()).thenReturn(customer);
        return feedback;
    }
}
