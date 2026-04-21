package edu.ap.bib_backend.service.review;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.dto.ReviewDashboardStatsDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.review.Review;
import edu.ap.bib_backend.jpa.entity.review.ReviewReport;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.mapper.ReviewMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewValidationService reviewValidationService;
    private final CurrentUserService currentUserService;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewReportRepository reviewReportRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper,
            ReviewValidationService reviewValidationService,
            CurrentUserService currentUserService
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
        this.reviewValidationService = reviewValidationService;
        this.currentUserService = currentUserService;
    }

    public List<ReviewDTO> getReviewsForBook(Long bookId, Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        List<ReviewModerationStatus> visibleStatuses = List.of(
                ReviewModerationStatus.ACTIVE,
                ReviewModerationStatus.KEPT
        );

        return reviewRepository.findVisibleReviewsByBookId(bookId, visibleStatuses).stream()
                .map(review -> reviewMapper.toDto(review, currentUserId))
                .toList();
    }

    public Page<ReviewDTO> getCurrentUserReviews(Authentication authentication, int page, int size) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return reviewRepository.findByUserId(currentUserId, pageable)
                .map(review -> reviewMapper.toDto(review, currentUserId));
    }

    public List<ReviewDTO> getAllReviews(Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        return reviewRepository.findAll().stream()
                .map(review -> reviewMapper.toDto(review, currentUserId))
                .toList();
    }

    public Page<ReviewDTO> getFlaggedReviews(Authentication authentication, int page, int size) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return reviewRepository.findByModerationStatus(ReviewModerationStatus.FLAGGED, pageable)
                .map(review -> reviewMapper.toDto(review, currentUserId));
    }

    public ReviewDTO addReviewToBook(Long bookId, ReviewDTO reviewDto, Authentication authentication) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("bookId", "Boek niet gevonden met id: " + bookId));

        reviewValidationService.validateReview(reviewDto);

        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + currentUserId));

        Review review = new Review(
                reviewDto.getComment(),
                reviewDto.getRating(),
                book,
                currentUser
        );

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toDto(savedReview, currentUserId);
    }

    public Double getAverageRating(Long bookId) {
        return reviewRepository.getAverageRatingForBook(bookId);
    }

    public void deleteReview(Long reviewId, Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("reviewId", "Review niet gevonden met id: " + reviewId));

        boolean isOwner = review.getUser() != null
                && review.getUser().getId() != null
                && review.getUser().getId().equals(currentUserId);

        if (!currentUserService.isModerator(authentication) && !isOwner) {
            throw new IllegalStateException("Je mag alleen je eigen review verwijderen.");
        }

        reviewRepository.delete(review);
    }

    public ReviewDTO reportReview(Long reviewId, Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("reviewId", "Review niet gevonden met id: " + reviewId));

        User reportedByUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + currentUserId));

        boolean hasAlreadyReported = reviewReportRepository.existsByReviewIdAndReportedById(reviewId, currentUserId);
        if (hasAlreadyReported) {
            throw new ResourceAlreadyExistsException(
                    "reviewId",
                    reviewId,
                    "Je hebt deze review al gerapporteerd."
            );
        }

        ReviewReport reviewReport = new ReviewReport(review, reportedByUser);
        reviewReportRepository.save(reviewReport);

        int uniqueReportCount = (int) reviewReportRepository.countByReviewId(reviewId);
        review.updateReportStatus(uniqueReportCount);

        Review updatedReview = reviewRepository.save(review);
        return reviewMapper.toDto(updatedReview, currentUserId);
    }

    public ReviewDTO keepReview(Long reviewId, Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("reviewId", "Review niet gevonden met id: " + reviewId));

        review.markAsKept();
        Review updatedReview = reviewRepository.save(review);

        return reviewMapper.toDto(updatedReview, currentUserId);
    }

    public ReviewDashboardStatsDTO getFlaggedReviewStats() {
        long totalFlaggedReviews = reviewRepository.countByModerationStatus(
                ReviewModerationStatus.FLAGGED
        );

        long criticalFlaggedReviews =
                reviewRepository.countByModerationStatusAndReportCountGreaterThanEqual(
                        ReviewModerationStatus.FLAGGED,
                        5
                );

        return new ReviewDashboardStatsDTO(
                totalFlaggedReviews,
                criticalFlaggedReviews
        );
    }
}