package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.dto.ReviewDashboardStatsDTO;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.service.review.ReviewService;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByBookId(@PathVariable Long bookId, Authentication authentication) {
        List<ReviewDTO> reviews = reviewService.getReviewsForBook(bookId, authentication);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> getAllReviews(Authentication authentication) {
        List<ReviewDTO> reviews = reviewService.getAllReviews(authentication);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/me")
    public ResponseEntity<PageDTO<ReviewDTO>> getCurrentUserReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewDTO> reviewPage = reviewService.getCurrentUserReviews(authentication, page, size);

        return ResponseEntity.ok(
                new PageDTO<>(
                        reviewPage.getContent(),
                        PageMapper.toPaginatedMeta(reviewPage)
                )
        );
    }

    @GetMapping("/flagged")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<PageDTO<ReviewDTO>> getFlaggedReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewDTO> flaggedReviewPage = reviewService.getFlaggedReviews(authentication, page, size);
        return ResponseEntity.ok(
            new PageDTO<>(
                flaggedReviewPage.getContent(), 
                PageMapper.toPaginatedMeta(flaggedReviewPage)
            )
        );
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<ReviewDTO> addReviewToBook(@PathVariable Long bookId, @RequestBody ReviewDTO reviewDto, Authentication authentication) {
        ReviewDTO savedReview = reviewService.addReviewToBook(bookId, reviewDto, authentication);
        return ResponseEntity.ok(savedReview);
    }

    @GetMapping("/book/{bookId}/average")
    public ResponseEntity<Double> getAverageRatingForBook(@PathVariable Long bookId) {
        Double averageRating = reviewService.getAverageRating(bookId);
        return ResponseEntity.ok(averageRating);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReviewById( @PathVariable Long reviewId, Authentication authentication) {
        reviewService.deleteReview(reviewId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reviewId}/report")
    public ResponseEntity<ReviewDTO> reportReview(@PathVariable Long reviewId, Authentication authentication) {
        ReviewDTO reportedReview = reviewService.reportReview(reviewId, authentication);
        return ResponseEntity.ok(reportedReview);
    }

    @PatchMapping("/{reviewId}/keep")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<ReviewDTO> keepReview( @PathVariable Long reviewId, Authentication authentication) {
        ReviewDTO keptReview = reviewService.keepReview(reviewId, authentication);
        return ResponseEntity.ok(keptReview);
    }

    @GetMapping("/flagged/stats")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<ReviewDashboardStatsDTO> getFlaggedReviewStats(Authentication authentication) {
        ReviewDashboardStatsDTO stats = reviewService.getFlaggedReviewStats(authentication);
        return ResponseEntity.ok(stats);
    }

}
