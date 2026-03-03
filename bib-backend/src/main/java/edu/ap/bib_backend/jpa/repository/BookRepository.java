package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}

