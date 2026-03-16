package edu.ap.bib_backend.jpa.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByISBN(String isbn);

    Book findByISBN(String isbn);

    @Query("SELECT b.ISBN FROM Book b")
    List<String> findAllIsbns();

    @Query("SELECT MIN(b.releaseYear) FROM Book b")
    Integer findMinReleaseYear();

    @Query("SELECT MAX(b.releaseYear) FROM Book b")
    Integer findMaxReleaseYear();

    @Query("SELECT MAX(b.pages) FROM Book b")
    Integer findMaxPages();

    @Query("""
            SELECT b FROM Book b
            JOIN b.bookCopies bc
            JOIN bc.loans bl
            JOIN bl.user u
            GROUP BY b
            ORDER BY COUNT(DISTINCT u.id) DESC
            LIMIT :limit
            """)
    List<Book> findMostReadBook(@Param("limit") int limit);

    @Query("SELECT b FROM Book b ORDER BY b.addedDate DESC")
    List<Book> findNewestBooks(Pageable pageable);
}
