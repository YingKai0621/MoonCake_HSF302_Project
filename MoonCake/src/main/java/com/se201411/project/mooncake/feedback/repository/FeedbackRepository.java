package com.se201411.project.mooncake.feedback.repository;

import com.se201411.project.mooncake.entity.Feedback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Feedback save(Feedback feedback);

    Optional<Feedback> findById(Long id);

    @EntityGraph(attributePaths = "customer")
    List<Feedback> findTop100ByOrderByIdDesc();

    @EntityGraph(attributePaths = "customer")
    List<Feedback> findTop20ByPublishedTrueAndRatingGreaterThanEqualOrderByCreatedAtDesc(int minimumRating);

    long countByPublishedFalseAndRatingIsNotNull();
}
