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
                        Integer releaseYear);

        boolean existsByIsbn(String isbn);

        Book findByIsbn(String isbn);

        @Query("SELECT b.isbn FROM Book b")
        List<String> findAllIsbns();

        @Query("SELECT MAX(b.pages) FROM Book b")
        Integer findMaxPages();

        @Query("SELECT MIN(b.pages) FROM Book b")
        Integer findMinPages();

        @Query("""
                        SELECT b FROM Book b
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
                SELECT DISTINCT b FROM Book b
                JOIN b.bookCopies bc
                WHERE bc.school.domainName = :schoolCode
                ORDER BY b.addedDate DESC
                """)
        List<Book> findNewestBooksBySchool(@Param("schoolCode") String schoolCode, Pageable pageable);

        @Query("""
                SELECT b FROM Book b
                JOIN b.bookCopies bc
                LEFT JOIN bc.loans bl
                LEFT JOIN bl.user u
                WHERE bc.school.domainName = :schoolCode
                GROUP BY b.id
                ORDER BY COUNT(DISTINCT u.id) DESC
                """)
        List<Book> findMostReadBookBySchool(@Param("schoolCode") String schoolCode, Pageable pageable);

        @Query("""
                        SELECT relatedBook, COUNT(sourceGenre) as sharedGenreCount
                        FROM Book sourceBook
                        JOIN sourceBook.genres sourceGenre
                        JOIN sourceGenre.books relatedBook
                        WHERE sourceBook.id = :bookId
                          AND relatedBook.id <> :bookId
                        GROUP BY relatedBook
                        ORDER BY COUNT(sourceGenre) DESC, RAND()
                        """)
        List<Object[]> findRelatedBooksWithScore(@Param("bookId") Long bookId, Pageable pageable);
}
