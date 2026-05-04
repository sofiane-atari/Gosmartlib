package edu.ap.bib_backend.jpa.repository.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.Loan;

public interface BookLoanRepository extends JpaRepository<Loan, Long> {

    void deleteByCopiesBookId(Long bookId);

    boolean existsByCopiesBookId(Long bookId);
}
