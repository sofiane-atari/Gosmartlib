package edu.ap.bib_backend.mapper;

import org.springframework.stereotype.Component;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.entity.review.Review;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;

@Component
public class ReviewMapper {

    private final ReviewReportRepository reviewReportRepository;

    public ReviewMapper(ReviewReportRepository reviewReportRepository) {
        this.reviewReportRepository = reviewReportRepository;
    }

    public ReviewDTO toDto(Review review, String currentUserId) {
        boolean reportedByCurrentUser = false;

        if (currentUserId != null) {
            reportedByCurrentUser = reviewReportRepository.existsByReviewIdAndReportedById(
                    review.getId(),
                    currentUserId
            );
        }

        ReviewDTO reviewDto = new ReviewDTO();
        reviewDto.setId(review.getId());
        reviewDto.setComment(review.getComment());
        reviewDto.setRating(review.getRating());
        reviewDto.setReportCount(review.getReportCount());
        reviewDto.setBookId(review.getBook().getId());
        reviewDto.setBookTitle(review.getBook().getTitle());
        reviewDto.setBookAuthor(review.getBook().getAuthor());
        reviewDto.setBookImageUrl(review.getBook().getImageUrl());
        reviewDto.setModerationStatus(review.getModerationStatus());
        reviewDto.setReportedByCurrentUser(reportedByCurrentUser);
        reviewDto.setCreatedAt(review.getCreatedAt());

        return reviewDto;
    }
}