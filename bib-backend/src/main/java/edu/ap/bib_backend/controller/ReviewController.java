package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService rs;

    public ReviewController(ReviewService rs){
        this.rs = rs;
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsForBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(rs.getReviewsForBook(bookId));
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<ReviewDTO> addReview(@PathVariable Long bookId, @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO savedReview = rs.addReviewToBook(bookId, reviewDTO);
        return ResponseEntity.ok(savedReview);
    }
    
}
