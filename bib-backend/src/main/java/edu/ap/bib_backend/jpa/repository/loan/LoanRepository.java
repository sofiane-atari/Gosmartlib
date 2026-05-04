package edu.ap.bib_backend.jpa.repository.loan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.ap.bib_backend.jpa.entity.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdOrderByLoanDateDesc(String userId);
}
