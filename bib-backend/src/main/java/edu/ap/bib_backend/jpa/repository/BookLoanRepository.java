package edu.ap.bib_backend.jpa.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.Loan;

public interface BookLoanRepository extends JpaRepository<Loan, Long> {

    void deleteByCopyId(Long copyId);

    void deleteByCopyIdIn(Collection<Long> copyIds);

    void deleteByCopyBookId(Long bookId);

    boolean existsByCopyBookId(Long bookId);

    Optional<Loan> findFirstByCopyIdAndReturnedDateIsNullOrderByReturnDateAsc(Long copyId);
}
