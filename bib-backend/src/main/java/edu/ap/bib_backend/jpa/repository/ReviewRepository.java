package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookId(Long bookId);
}
