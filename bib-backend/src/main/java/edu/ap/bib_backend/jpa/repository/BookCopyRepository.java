package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.enums.Availability;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    boolean existsByBookIdAndAvailability(Long bookId, Availability availability);

    void deleteByBookId(Long bookId);

    long countBySchoolId(Long schoolId);

    @Query("""
            SELECT bc
            FROM BookCopy bc
            LEFT JOIN bc.loans bl
            WHERE bc.book.id = :bookId
            GROUP BY bc
            HAVING SUM(CASE WHEN bl.id IS NOT NULL AND bl.returnedDate IS NULL THEN 1 ELSE 0 END) = 0
            """)
    java.util.List<BookCopy> findAvailableCopiesByBookId(@Param("bookId") Long bookId);

}
