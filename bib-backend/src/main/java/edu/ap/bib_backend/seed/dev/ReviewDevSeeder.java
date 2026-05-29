package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.service.review.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ReviewDevSeeder {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    public ReviewDevSeeder(ReviewService reviewService, ReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
    }

    private static final List<ReviewData> REVIEW_COMMENTS = List.of(
            new ReviewData("Erg leuk en leerzaam boek! Zeker de moeite waard.", 5),
            new ReviewData("Goed geschreven, makkelijk te volgen.", 4),
            new ReviewData("Nuttige informatie, maar soms wat droog.", 3),
            new ReviewData("Fantastisch boek, ik heb er veel van geleerd.", 5),
            new ReviewData("Redelijk interessant, maar niet wat ik verwachtte.", 3),
            new ReviewData("Zeer praktisch en direct toepasbaar.", 4),
            new ReviewData("Moeilijk om doorheen te komen, maar wel degelijk.", 2),
            new ReviewData("Perfect voor beginners in dit vakgebied.", 4),
            new ReviewData("Een must-read voor elke ontwikkelaar.", 5),
            new ReviewData("Iets te theoretisch naar mijn smaak.", 3),
            new ReviewData("Praktische voorbeelden maken het heel duidelijk.", 4),
            new ReviewData("Geweldige referentie om telkens weer raad te plegen.", 5),
            new ReviewData("De uitleg is helder en goed gestructureerd.", 4),
            new ReviewData("Had wat meer voorbeelden kunnen gebruiken.", 3),
            new ReviewData("Veruit het beste boek over dit onderwerp.", 5)
    );

    public ReviewSeedResult seed(List<Long> bookIds, List<String> studentIds) {
        if (studentIds.isEmpty() || bookIds.isEmpty()) {
            log.info("Geen studenten of boeken gevonden, review seeding overgeslagen");
            return new ReviewSeedResult(0, 0, List.of());
        }

        int reviewsCreated = 0;
        int reviewsReported = 0;
        List<Long> createdReviewIds = new ArrayList<>();
        int commentIndex = 0;

        int booksToReview = Math.min(bookIds.size(), 10);
        for (int i = 0; i < booksToReview; i++) {
            Long bookId = bookIds.get(i);

            int reviewersPerBook = Math.min(3, studentIds.size());
            for (int j = 0; j < reviewersPerBook; j++) {
                String studentId = studentIds.get((i + j) % studentIds.size());

                if (reviewRepository.existsByBookIdAndUserId(bookId, studentId)) {
                    log.debug("Review overgeslagen (bestaat al): boek={}, student={}", bookId, studentId);
                    continue;
                }

                ReviewData reviewData = REVIEW_COMMENTS.get(commentIndex % REVIEW_COMMENTS.size());
                commentIndex++;

                Authentication studentAuth = DevSeedAuthentication.forUser(studentId, Role.STUDENT);

                try {
                    ReviewDTO reviewDto = new ReviewDTO();
                    reviewDto.setComment(reviewData.comment);
                    reviewDto.setRating(reviewData.rating);

                    ReviewDTO created = reviewService.addReviewToBook(bookId, reviewDto, studentAuth);
                    reviewsCreated++;
                    createdReviewIds.add(created.getId());
                    log.info("Review aangemaakt: boek={}, student={}", bookId, studentId);
                } catch (Exception e) {
                    log.warn("Kon review niet aanmaken: boek={}, student={}: {}",
                            bookId, studentId, e.getMessage());
                }
            }
        }

        if (reviewsCreated >= 3) {
            List<Long> allBookIds = bookIds.subList(0, Math.min(2, bookIds.size()));
            for (Long bookId : allBookIds) {
                String reporterId = studentIds.get(studentIds.size() - 1);
                Authentication reporterAuth = DevSeedAuthentication.forUser(reporterId, Role.STUDENT);

                try {
                    var reviews = reviewRepository.findVisibleReviewsByBookId(bookId,
                            List.of(ReviewModerationStatus.ACTIVE, ReviewModerationStatus.FLAGGED));

                    if (!reviews.isEmpty()) {
                        Long reviewId = reviews.get(0).getId();
                        reviewService.reportReview(reviewId, reporterAuth);
                        reviewsReported++;
                        log.info("Review gerapporteerd: reviewId={} door {}", reviewId, reporterId);
                    }
                } catch (Exception e) {
                    log.warn("Kon review niet rapporteren: {}", e.getMessage());
                }
            }
        }

        log.info("Review seeding voltooid: {} reviews aangemaakt, {} gerapporteerd",
                reviewsCreated, reviewsReported);
        return new ReviewSeedResult(reviewsCreated, reviewsReported, createdReviewIds);
    }

    public record ReviewData(String comment, int rating) {}

    public record ReviewSeedResult(int reviewsCreated, int reviewsReported, List<Long> reviewIds) {}
}