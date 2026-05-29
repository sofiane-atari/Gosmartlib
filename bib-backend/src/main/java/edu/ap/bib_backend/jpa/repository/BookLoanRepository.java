package edu.ap.bib_backend.jpa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Loan;

public interface BookLoanRepository extends JpaRepository<Loan, Long> {

    void deleteByCopiesBookId(Long bookId);

    boolean existsByCopiesBookId(Long bookId);

    @Query(value = "select distinct lc.loan_id from loan_copies lc where lc.book_copy_id = :copyId", nativeQuery = true)
    List<Long> findLoanIdsByCopyId(@Param("copyId") Long copyId);

    @Query(value = "select distinct lc.loan_id from loan_copies lc where lc.book_copy_id in (:copyIds)", nativeQuery = true)
    List<Long> findLoanIdsByCopyIds(@Param("copyIds") Collection<Long> copyIds);

    @Modifying
    @Query(value = "delete from loan_copies where book_copy_id = :copyId", nativeQuery = true)
    int deleteLoanCopiesByCopyId(@Param("copyId") Long copyId);

    @Modifying
    @Query(value = "delete from loan_copies where book_copy_id in (:copyIds)", nativeQuery = true)
    int deleteLoanCopiesByCopyIds(@Param("copyIds") Collection<Long> copyIds);

    @Modifying
    @Query(value = "delete from loans where id in (:loanIds) and not exists (select 1 from loan_copies lc where lc.loan_id = loans.id)", nativeQuery = true)
    int deleteOrphanLoansByIds(@Param("loanIds") Collection<Long> loanIds);
}
