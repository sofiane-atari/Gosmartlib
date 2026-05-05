package edu.ap.bib_backend.jpa.repository.loan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.loan.LoanExtension;

public interface LoanExtensionRepository extends JpaRepository<LoanExtension, Long> {

    List<LoanExtension> findByLoanIdOrderByExtendedAtDesc(Long loanId);

    int countByLoanId(Long loanId);
}