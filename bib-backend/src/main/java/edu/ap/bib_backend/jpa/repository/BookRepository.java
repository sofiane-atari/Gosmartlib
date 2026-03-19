package edu.ap.bib_backend.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {


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
            LIMIT :limit
            """)
    List<Book> findMostReadBook(@Param("limit") int limit);

    @Query("SELECT b FROM Book b ORDER BY b.addedDate DESC")
    List<Book> findNewestBooks(Pageable pageable);
}
