package edu.ap.bib_backend.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByTitleIgnoreCaseAndAuthorIgnoreCaseAndReleaseYear(
            String title,
            String author,
            Integer releaseYear
    );

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndCampusId(String isbn, Long campusId);

    Book findByIsbn(String isbn);

    Optional<Book> findByIsbnAndCampusId(String isbn, Long campusId);

    Optional<Book> findByIsbnAndCampusSchoolId(String isbn, Long schoolId);

    Optional<Book> findByIdAndCampusId(Long id, Long campusId);

    Optional<Book> findByIdAndCampusSchoolId(Long id, Long schoolId);

    List<Book> findByCampusId(Long campusId);

    @Query("SELECT b.isbn FROM Book b")
    List<String> findAllIsbns();

    @Query("SELECT MAX(b.pages) FROM Book b")
    Integer findMaxPages();

    @Query("SELECT MIN(b.pages) FROM Book b")
    Integer findMinPages();

    @Query("""
            SELECT b
            FROM Book b
            LEFT JOIN b.bookCopies bc
            LEFT JOIN bc.loans bl
            LEFT JOIN bl.user u
            GROUP BY b.id
            ORDER BY COUNT(DISTINCT u.id) DESC
            """)
    List<Book> findMostReadBook(Pageable pageable);

    @Query("SELECT b FROM Book b ORDER BY b.addedDate DESC")
    List<Book> findNewestBooks(Pageable pageable);

    @Query("""
            SELECT b
            FROM Book b
            WHERE b.campus.id = :campusId
            ORDER BY b.addedDate DESC
            """)
    List<Book> findNewestBooksByCampus(@Param("campusId") Long campusId, Pageable pageable);

    @Query("""
            SELECT b
            FROM Book b
            LEFT JOIN b.bookCopies bc
            LEFT JOIN bc.loans bl
            LEFT JOIN bl.user u
            WHERE b.campus.id = :campusId
            GROUP BY b
            ORDER BY COUNT(DISTINCT u.id) DESC
            """)
    List<Book> findMostReadBookByCampus(@Param("campusId") Long campusId, Pageable pageable);

    @Query("""
            SELECT b
            FROM Book b
            LEFT JOIN b.bookCopies bc
            LEFT JOIN bc.loans bl
            LEFT JOIN bl.user u
            WHERE b.campus.school.id = :schoolId
            GROUP BY b
            ORDER BY COUNT(DISTINCT u.id) DESC
            """)
    List<Book> findMostReadBookBySchool(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("""
            SELECT b
            FROM Book b
            WHERE b.campus.school.id = :schoolId
            ORDER BY b.addedDate DESC
            """)
    List<Book> findNewestBooksBySchool(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("""
            SELECT relatedBook, COUNT(sourceGenre) AS sharedGenreCount
            FROM Book sourceBook
            JOIN sourceBook.genres sourceGenre
            JOIN sourceGenre.books relatedBook
            WHERE sourceBook.id = :bookId
              AND relatedBook.id <> :bookId
              AND relatedBook.campus.id = :campusId
            GROUP BY relatedBook
            ORDER BY COUNT(sourceGenre) DESC, RAND()
            """)
    List<Object[]> findRelatedBooksWithScore(
            @Param("bookId") Long bookId,
            @Param("campusId") Long campusId,
            Pageable pageable
    );

    @Query("""
            SELECT relatedBook, COUNT(sourceGenre) AS sharedGenreCount
            FROM Book sourceBook
            JOIN sourceBook.genres sourceGenre
            JOIN sourceGenre.books relatedBook
            WHERE sourceBook.id = :bookId
              AND relatedBook.id <> :bookId
              AND relatedBook.campus.school.id = :schoolId
            GROUP BY relatedBook
            ORDER BY COUNT(sourceGenre) DESC, RAND()
            """)
    List<Object[]> findRelatedBooksWithScoreBySchool(
            @Param("bookId") Long bookId,
            @Param("schoolId") Long schoolId,
            Pageable pageable
    );
}
