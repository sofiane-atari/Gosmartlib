package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.ap.bib_backend.jpa.entity.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {

}
