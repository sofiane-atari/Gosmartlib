package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
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
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ProfanityFilterService profanityFilterService;

    public ReviewService(   ReviewRepository reviewRepository, ReviewReportRepository reviewReportRepository, 
                            BookRepository bookRepository, UserRepository userRepository, ProfanityFilterService profanityFilterService
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.profanityFilterService = profanityFilterService;
    }

    public List<ReviewDTO> getReviewsForBook(Long bookId, Authentication authentication) {
        List<Review> reviews = reviewRepository.findByBookId(bookId);
        String currentUserId = extractCurrentUserId(authentication);

        return reviews.stream()
                .map(review -> mapToReviewDto(review, currentUserId))
                .toList();
    }

    public List<ReviewDTO> getCurrentUserReviews(Authentication authentication) {
        String currentUserId = extractCurrentUserId(authentication);

        return reviewRepository.findByUserId(currentUserId).stream()
                .map(review -> mapToReviewDto(review, currentUserId))
                .toList();
    }

    public List<ReviewDTO> getAllReviews(Authentication authentication) {
        String currentUserId = extractCurrentUserId(authentication);

        return reviewRepository.findAll().stream()
                .map(review -> mapToReviewDto(review, currentUserId))
                .toList();
    }

    public List<ReviewDTO> getFlaggedReviews(Authentication authentication) {
        String currentUserId = extractCurrentUserId(authentication);

        return reviewRepository.findByModerationStatus(ReviewModerationStatus.FLAGGED).stream()
                .map(review -> mapToReviewDto(review, currentUserId))
                .toList();
    }

    public ReviewDTO addReviewToBook(Long bookId, ReviewDTO reviewDto, Authentication authentication) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("bookId", "Boek niet gevonden met id: " + bookId));

        validateReview(reviewDto);

        String currentUserId = extractCurrentUserId(authentication);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + currentUserId));

        Review review = new Review(
                reviewDto.getComment(),
                reviewDto.getRating(),
                book,
                currentUser
        );
        Review savedReview = reviewRepository.save(review);

        return mapToReviewDto(savedReview, currentUserId);
    }

    public Double getAverageRating(Long bookId) {
        return reviewRepository.getAverageRatingForBook(bookId);
    }

    public void deleteReview(Long reviewId, Authentication authentication) {
        String currentUserId = extractCurrentUserId(authentication);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("reviewId", "Review niet gevonden met id: " + reviewId));

        boolean isOwner = review.getUser() != null
                && review.getUser().getId() != null
                && review.getUser().getId().equals(currentUserId);

        if (!isModerator(authentication) && !isOwner) {
            throw new IllegalStateException("Je mag alleen je eigen review verwijderen.");
        }

        reviewRepository.delete(review);
    }

    public ReviewDTO reportReview(Long reviewId, Authentication authentication) {
        String currentUserId = extractCurrentUserId(authentication);

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

        return mapToReviewDto(updatedReview, currentUserId);
    }

    public ReviewDTO keepReview(Long reviewId, Authentication authentication) {
        String currentUserId = extractCurrentUserId(authentication);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("reviewId", "Review niet gevonden met id: " + reviewId));

        review.markAsKept();
        Review updatedReview = reviewRepository.save(review);

        return mapToReviewDto(updatedReview, currentUserId);
    }

    private void validateReview(ReviewDTO reviewDto) {
        if (reviewDto == null) {
            throw new IllegalArgumentException("Review mag niet leeg zijn.");
        }

        if (reviewDto.getComment() == null || reviewDto.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Reviewcommentaar is verplicht.");
        }

        if (reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
            throw new IllegalArgumentException("Rating moet tussen 1 en 5 liggen.");
        }

        if (profanityFilterService.containsProfanity(reviewDto.getComment())) {
            throw new IllegalArgumentException("Je review bevat ongepast taalgebruik en kan niet opgeslagen worden.");
        }
    }

    private ReviewDTO mapToReviewDto(Review review, String currentUserId) {
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
        reviewDto.setModerationStatus(review.getModerationStatus());
        reviewDto.setReportedByCurrentUser(reportedByCurrentUser);
        reviewDto.setCreatedAt(review.getCreatedAt());

        return reviewDto;
    }

    private String extractCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Geen geauthenticeerde gebruiker gevonden.");
        }

        return authentication.getName();
    }

    private boolean isModerator(Authentication authentication) {
    return authentication.getAuthorities().stream()
            .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_LIBRARIAN")
                            || authority.getAuthority().equals("ROLE_ADMIN")
            );
}
}