package edu.ap.bib_backend.jpa.repository.review;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.review.ReviewReport;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReportedById(Long reviewId, String reportedByUserId);

    long countByReviewId(Long reviewId);

    void deleteByReviewId(Long reviewId);
}
