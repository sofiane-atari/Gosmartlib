package edu.ap.bib_backend.jpa.repository;

import java.time.LocalDate;
import java.util.List;

import edu.ap.bib_backend.jpa.entity.Loan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.projection.report.PopularGenreProjection;

public interface LoanRepository extends JpaRepository<Loan, Long> {

        @Query("""
                select l
                from Loan l
                where l.user.id = :userId
                and l.campus.id = :campusId
                and l.returnedDate is null
                order by l.loanDate desc
                """)
        List<Loan> findActiveHistoryLoans(
                @Param("userId") String userId,
                @Param("campusId") Long campusId
        );

        @Query("""
                select l
                from Loan l
                where l.user.id = :userId
                and l.campus.id = :campusId
                and l.returnedDate is not null
                order by l.returnedDate desc, l.loanDate desc
                """)
        Page<Loan> findReturnedHistoryLoans(
                @Param("userId") String userId,
                @Param("campusId") Long campusId,
                Pageable pageable
        );

        List<Loan> findByCampusSchoolId(Long schoolId);

        List<Loan> findByCampusSchoolIdAndReturnedDateIsNull(Long schoolId);

        List<Loan> findByUserIdOrderByLoanDateDesc(String userId);

        long countByUserId(String userId);

        long countByUserIdAndReturnedDateIsNull(String userId);
        @Query("""
                select count(l)
                from Loan l
                where l.user.id = :userId
                and l.campus.id = :campusId
                and l.returnedDate is null
                """)
        long countActiveHistoryLoans(
                @Param("userId") String userId,
                @Param("campusId") Long campusId
        );

        @Query("""
                select count(l)
                from Loan l
                where l.user.id = :userId
                and l.campus.id = :campusId
                and l.returnedDate is not null
                """)
        long countReturnedHistoryLoans(
                @Param("userId") String userId,
                @Param("campusId") Long campusId
        );

        @Query(value = """
                select count(*)
                from loans l
                join loan_copies lc on lc.loan_id = l.id
                where l.user_id = :userId
                and l.campus_id = :campusId
                """, nativeQuery = true)
        long countLoanedBookCopiesForUserHistory(
                @Param("userId") String userId,
                @Param("campusId") Long campusId
        );


        List<Loan> findByCampusId(Long campusId);

        List<Loan> findByCampusIdAndReturnedDateIsNull(Long id);

        @Query(value = """
                        select count(*)
                        from loans l
                        join loan_copies lc on lc.loan_id = l.id
                        where l.user_id = :userId
                        """, nativeQuery = true)
        long countLoanedBooksForUser(@Param("userId") String userId);

        @Query(value = """
                        select count(*)
                        from loans l
                        join loan_copies lc on lc.loan_id = l.id
                        where l.user_id = :userId
                        and l.returned_date is null
                        """, nativeQuery = true)
        long countActiveLoanedBooksForUser(@Param("userId") String userId);

        @Query(value = """
                        select count(*)
                        from loans l
                        join loan_copies lc on lc.loan_id = l.id
                        where l.user_id = :userId
                        and l.returned_date is not null
                        """, nativeQuery = true)
        long countReturnedLoanedBooksForUser(@Param("userId") String userId);

        @Query(value = """
                        select g.id as genreId,
                               g.name as name,
                               g.color as color,
                               count(*) as loanCount
                        from loans l
                        join loan_copies lc on lc.loan_id = l.id
                        join book_copies bc on bc.id = lc.book_copy_id
                        join book_genres bg on bg.book_id = bc.book_id
                        join genres g on g.id = bg.genre_id
                        where l.user_id in (:userIds)
                        group by g.id, g.name, g.color
                        order by loanCount desc, g.name asc
                        limit 6
                        """, nativeQuery = true)
        List<PopularGenreProjection> findPopularGenresForUsers(@Param("userIds") List<String> userIds);

        long countByReturnedDateIsNullAndReturnDateGreaterThanEqual(LocalDate today);

        long countByReturnedDateIsNullAndReturnDateBefore(LocalDate today);

        long countByReturnedDateIsNotNull();


}
