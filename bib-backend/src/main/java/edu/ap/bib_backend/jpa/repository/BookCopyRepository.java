package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.ap.bib_backend.jpa.entity.BookCopy;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

}
