package edu.ap.bib_backend.jpa.repository.review;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.review.Review;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookId(Long bookId);

    Page<Review> findByUserId(String userId, Pageable pageable);

    void deleteByBookId(Long bookId);

    Page<Review> findByModerationStatus(ReviewModerationStatus moderationStatus, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingForBook(@Param("bookId") Long bookId);
}
