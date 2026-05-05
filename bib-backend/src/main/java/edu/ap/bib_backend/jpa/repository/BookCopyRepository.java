package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import edu.ap.bib_backend.jpa.entity.BookCopy;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END
            FROM BookCopy bc
            JOIN bc.loans bl
            WHERE bc.book.id = :bookId
              AND bl.returnedDate IS NULL
            """)
    boolean existsByBookIdAndActiveLoan(@Param("bookId") Long bookId);

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
