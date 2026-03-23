package edu.ap.bib_backend.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByISBN(String isbn);

    Book findByISBN(String isbn);

    @Query("SELECT b.ISBN FROM Book b")
    List<String> findAllIsbns();

    @Query("SELECT MAX(b.pages) FROM Book b")
    Integer findMaxPages();

    @Query("SELECT MIN(b.pages) FROM Book b")
    Integer findMinPages();

    @Query("""
            SELECT b FROM Book b
            JOIN b.bookCopies bc
            JOIN bc.loans bl
            JOIN bl.user u
            GROUP BY b
            ORDER BY COUNT(DISTINCT u.id) DESC
            """)
    List<Book> findMostReadBook(Pageable pageable);

    @Query("SELECT b FROM Book b ORDER BY b.addedDate DESC")
    List<Book> findNewestBooks(Pageable pageable);



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
