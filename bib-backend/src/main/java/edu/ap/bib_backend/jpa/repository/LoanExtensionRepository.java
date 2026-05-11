package edu.ap.bib_backend.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.LoanExtension;

public interface LoanExtensionRepository extends JpaRepository<LoanExtension, Long> {

    List<LoanExtension> findByLoanIdOrderByExtendedAtDesc(Long loanId);

    int countByLoanId(Long loanId);
}