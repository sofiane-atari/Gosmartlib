package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Review;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository rr;
    private final BookRepository br;
    private final ProfanityFilterService ps;

    public ReviewService(ReviewRepository rr, BookRepository br, ProfanityFilterService ps) {
        this.rr = rr;
        this.br = br;
        this.ps = ps;
    }

    public List<ReviewDTO> getReviewsForBook(Long bookId) {
        return rr.findByBookId(bookId).stream()
                .map(review -> new ReviewDTO(review.getId(), review.getComment(), review.getRating(),
                        review.getReportCount(), review.getBook().getId(), review.getBook().getTitle(),
                        review.getBook().getAuthor()))
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getAllReviews() {
        return rr.findAll().stream()
                .map(review -> new ReviewDTO(review.getId(), review.getComment(), review.getRating(),
                        review.getReportCount(), review.getBook().getId(), review.getBook().getTitle(),
                        review.getBook().getAuthor()))
                .collect(Collectors.toList());
    }

    public ReviewDTO addReviewToBook(Long bookId, ReviewDTO reviewDTO) {
        if (ps.containsProfanity(reviewDTO.getComment())) {
            throw new RuntimeException("Review contains profanity");
        }

        Book book = br.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Didn't find book with id: " + bookId));

        Review review = new Review(reviewDTO.getComment(), reviewDTO.getRating(), book);
        Review savedReview = rr.save(review);

        return new ReviewDTO(savedReview.getId(), savedReview.getComment(), savedReview.getRating(),
                savedReview.getReportCount(), savedReview.getBook().getId(), savedReview.getBook().getTitle(),
                savedReview.getBook().getAuthor());
    }

    public Double getAverageRating(Long bookId) {
        return rr.getAverageRatingForBook(bookId);
    }

    public void deleteReview(Long reviewId) {

        Review review = rr.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Didn't find review with id: " + reviewId));

        rr.delete(review);
    }

    public ReviewDTO reportReview(Long reviewId) {
        Review review = rr.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Didn't find review with id: " + reviewId));

        review.setReportCount(review.getReportCount() + 1);
        Review savedReview = rr.save(review);

        return new ReviewDTO(savedReview.getId(), savedReview.getComment(), savedReview.getRating(),
                savedReview.getReportCount(), savedReview.getBook().getId(), savedReview.getBook().getTitle(),
                savedReview.getBook().getAuthor());
    }

}