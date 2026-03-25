package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.enums.Availability;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    boolean existsByBookIdAndAvailability(Long bookId, Availability availability);

    void deleteByBookId(Long bookId);

}
