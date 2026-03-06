package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Review;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

@Service
public class ReviewService {

    private final ReviewRepository rr;
    private final BookRepository br; 

    public ReviewService(ReviewRepository rr, BookRepository br) {
        this.rr = rr;
        this.br = br;
    }

    public List<ReviewDTO> getReviewsForBook(Long bookId) {
        return rr.findByBookId(bookId).stream()
                .map(review -> new ReviewDTO(review.getId(), review.getComment(), review.getRating()))
                .collect(Collectors.toList());
    }

    public ReviewDTO addReviewToBook(Long bookId, ReviewDTO reviewDTO) {
        Book book = br.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + bookId));
        
        Review review = new Review(reviewDTO.getComment(), reviewDTO.getRating(), book);
        Review savedReview = rr.save(review);
        
        return new ReviewDTO(savedReview.getId(), savedReview.getComment(), savedReview.getRating());
    }

    public Double getAverageRating(Long bookId) {
        return rr.getAverageRatingForBook(bookId);
    }


    public void deleteReview(Long reviewId){

        Review review = rr.findById(reviewId)
        .orElseThrow(() -> new RuntimeException("Review niet gevonden met id: " + reviewId));

        rr.delete(review);
    }

    
}