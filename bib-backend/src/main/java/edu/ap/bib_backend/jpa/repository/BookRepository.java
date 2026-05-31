package edu.ap.bib_backend.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Book;
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findByTitleIgnoreCaseAndAuthorIgnoreCaseAndReleaseYear(
            String title,
            String author,
            Integer releaseYear
    );

    boolean existsByIsbn(String isbn);

    Book findByIsbn(String isbn);

    List<Book> findAllByIsbn(String isbn);

    @Query("SELECT b.isbn FROM Book b")
    List<String> findAllIsbns();

    @Query("SELECT MAX(b.pages) FROM Book b")
    Integer findMaxPages();

    @Query("SELECT MIN(b.pages) FROM Book b")
    Integer findMinPages();

    @Query(value = """
            select count(distinct case
                when nullif(trim(b.isbn), '') is not null then concat('isbn:', trim(b.isbn))
                else concat('title:', lower(trim(b.title)))
            end)
            from books b
            """, nativeQuery = true)
    long countUniqueTitlesForSystem();

    @Query("""
            SELECT b
            FROM Book b
            LEFT JOIN b.bookCopies bc
            LEFT JOIN Loan bl ON bl.copy = bc
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
            LEFT JOIN b.bookCopies bc
            LEFT JOIN Loan bl ON bl.copy = bc
            LEFT JOIN bl.user u
            WHERE bc.campus.school.id = :schoolId
            GROUP BY b
            ORDER BY COUNT(DISTINCT u.id) DESC
            """)
    List<Book> findMostReadBookBySchool(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("""
            SELECT DISTINCT b
            FROM Book b
            JOIN b.bookCopies bc
            WHERE bc.campus.school.id = :schoolId
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
              AND (:schoolDomain IS NULL OR EXISTS (
                  SELECT 1 FROM BookCopy bc
                  WHERE bc MEMBER OF relatedBook.bookCopies
                    AND bc.campus.school.domainName = :schoolDomain
              ))
            GROUP BY relatedBook
            ORDER BY COUNT(sourceGenre) DESC, RAND()
            """)
    List<Object[]> findRelatedBooksWithScoreBySchoolDomain(
            @Param("bookId") Long bookId,
            @Param("schoolDomain") String schoolDomain,
            Pageable pageable
    );

    @Query("""
            SELECT relatedBook, COUNT(sourceGenre) AS sharedGenreCount
            FROM Book sourceBook
            JOIN sourceBook.genres sourceGenre
            JOIN sourceGenre.books relatedBook
            WHERE sourceBook.id = :bookId
              AND relatedBook.id <> :bookId
              AND EXISTS (
                  SELECT 1 FROM BookCopy bc
                  WHERE bc MEMBER OF relatedBook.bookCopies
                    AND bc.campus.school.id = :schoolId
              )
            GROUP BY relatedBook
            ORDER BY COUNT(sourceGenre) DESC, RAND()
            """)
    List<Object[]> findRelatedBooksWithScoreBySchool(
            @Param("bookId") Long bookId,
            @Param("schoolId") Long schoolId,
            Pageable pageable
    );
}
