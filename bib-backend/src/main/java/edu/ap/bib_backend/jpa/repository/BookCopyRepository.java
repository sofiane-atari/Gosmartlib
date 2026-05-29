package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.enums.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END
            FROM BookCopy bc
            JOIN bc.loans bl
            WHERE bc.book.id = :bookId
              AND bl.returnedDate IS NULL
            """)
    boolean existsByBookIdAndActiveLoan(@Param("bookId") Long bookId);

    boolean existsByBookIdAndCampusIdAndAvailability(Long bookId, Long campusId, Availability availability);

    void deleteByBookId(Long bookId);

    long countByCampusId(Long campusId);

    long countByBookId(Long bookId);

    long countByCampusSchoolId(Long schoolId);

    Optional<BookCopy> findFirstByBookIdAndCampusIdAndAvailability(
            Long bookId,
            Long campusId,
            Availability availability);

    @Query("""
            SELECT bc
            FROM BookCopy bc
            LEFT JOIN bc.loans bl
            WHERE bc.book.id = :bookId
              AND bc.condition <> edu.ap.bib_backend.jpa.enums.BookCondition.LOST
            GROUP BY bc
            HAVING SUM(CASE WHEN bl.id IS NOT NULL AND bl.returnedDate IS NULL THEN 1 ELSE 0 END) = 0
            """)
    java.util.List<BookCopy> findAvailableCopiesByBookId(@Param("bookId") Long bookId);

    @Query("""
            SELECT bc
            FROM BookCopy bc
            LEFT JOIN bc.loans bl
            WHERE bc.book.id = :bookId
              AND bc.campus.id = :campusId
              AND bc.condition <> edu.ap.bib_backend.jpa.enums.BookCondition.LOST
            GROUP BY bc
            HAVING SUM(CASE WHEN bl.id IS NOT NULL AND bl.returnedDate IS NULL THEN 1 ELSE 0 END) = 0
            """)
    java.util.List<BookCopy> findAvailableCopiesByBookIdAndCampusId(@Param("bookId") Long bookId, @Param("campusId") Long campusId);

    @Query("""
            SELECT bc
            FROM BookCopy bc
            JOIN bc.campus c
            WHERE bc.book.id = :bookId
              AND c.school.id = :schoolId
            """)
    java.util.List<BookCopy> findAllByBookIdAndCampusSchoolId(@Param("bookId") Long bookId, @Param("schoolId") Long schoolId);
}
