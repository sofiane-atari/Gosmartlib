package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.enums.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    boolean existsByBookIdAndAvailability(Long bookId, Availability availability);

    boolean existsByBookIdAndCampusIdAndAvailability(Long bookId, Long campusId, Availability availability);

    void deleteByBookId(Long bookId);

    long countByCampusId(Long campusId);

    long countByCampusSchoolId(Long schoolId);

    Optional<BookCopy> findFirstByBookIdAndCampusIdAndAvailability(
            Long bookId,
            Long campusId,
            Availability availability
    );
}