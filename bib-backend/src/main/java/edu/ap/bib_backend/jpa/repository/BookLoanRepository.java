package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.BookLoan;

public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    void deleteByCopyBookId(Long bookId);

    boolean existsByCopyBookId(Long bookId);
}
