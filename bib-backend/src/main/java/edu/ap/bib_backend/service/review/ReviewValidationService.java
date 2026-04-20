package edu.ap.bib_backend.service.review;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.service.ProfanityFilterService;

@Service
public class ReviewValidationService {

    private final ProfanityFilterService profanityFilterService;

    public ReviewValidationService(ProfanityFilterService profanityFilterService) {
        this.profanityFilterService = profanityFilterService;
    }

    public void validateReview(ReviewDTO reviewDto) {
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
}