package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.ap.bib_backend.jpa.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT MIN(b.releaseYear) FROM Book b")
    Integer findMinReleaseYear();

    @Query("SELECT MAX(b.releaseYear) FROM Book b")
    Integer findMaxReleaseYear();

    @Query("SELECT MAX(b.pages) FROM Book b")
    Integer findMaxPages();

}
