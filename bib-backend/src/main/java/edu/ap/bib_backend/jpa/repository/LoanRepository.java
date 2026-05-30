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
import edu.ap.bib_backend.projection.report.ClassLoanActivityProjection;
import edu.ap.bib_backend.projection.report.LoanVolumePeriodProjection;
import edu.ap.bib_backend.projection.report.PopularLoanedBookProjection;

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
                        where l.user_id = :userId
                        """, nativeQuery = true)
        long countLoanedBooksForUser(@Param("userId") String userId);

        @Query(value = """
                        select count(*)
                        from loans l
                        where l.user_id = :userId
                        and l.returned_date is null
                        """, nativeQuery = true)
        long countActiveLoanedBooksForUser(@Param("userId") String userId);

        @Query(value = """
                        select count(*)
                        from loans l
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
                        join book_copies bc on bc.id = l.book_copy_id
                        join book_genres bg on bg.book_id = bc.book_id
                        join genres g on g.id = bg.genre_id
                        where l.user_id in (:userIds)
                        group by g.id, g.name, g.color
                        order by loanCount desc, g.name asc
                        limit 6
                        """, nativeQuery = true)
        List<PopularGenreProjection> findPopularGenresForUsers(@Param("userIds") List<String> userIds);

        @Query(value = """
                        select g.id as genreId,
                               g.name as name,
                               g.color as color,
                               count(*) as loanCount
                        from loans l
                        join users u on u.id = l.user_id
                        join auth_sessions s on s.user_id = u.id
                        join book_copies bc on bc.id = l.book_copy_id
                        join book_genres bg on bg.book_id = bc.book_id
                        join genres g on g.id = bg.genre_id
                        where u.school_id = :schoolId
                        and s.smartschool_refresh_token is not null
                        and (s.smartschool_refresh_token_expires_at is null or s.smartschool_refresh_token_expires_at > current_timestamp)
                        and (:campusId is null or l.campus_id = :campusId)
                        group by g.id, g.name, g.color
                        order by loanCount desc, g.name asc
                        """, nativeQuery = true)
        List<PopularGenreProjection> findPopularGenresForSchool(
                @Param("schoolId") Long schoolId,
                @Param("campusId") Long campusId,
                Pageable pageable);

        @Query("""
                        select bc.book.id as bookId,
                               bc.book.title as title,
                               bc.book.author as author,
                               bc.book.imageUrl as imageUrl,
                               count(bc.id) as loanCount
                        from Loan l
                        join l.copy bc
                        where l.campus.school.id = :schoolId
                        and (:campusId is null or l.campus.id = :campusId)
                        group by bc.book.id, bc.book.title, bc.book.author, bc.book.imageUrl
                        order by count(bc.id) desc, bc.book.title asc
                        """)
        List<PopularLoanedBookProjection> findPopularLoanedBooksForSchool(
                @Param("schoolId") Long schoolId,
                @Param("campusId") Long campusId,
                Pageable pageable);

        @Query("""
                        select bc.book.id as bookId,
                               bc.book.title as title,
                               bc.book.author as author,
                               bc.book.imageUrl as imageUrl,
                               count(bc.id) as loanCount
                        from Loan l
                        join l.copy bc
                        group by bc.book.id, bc.book.title, bc.book.author, bc.book.imageUrl
                        order by count(bc.id) desc, bc.book.title asc
                        """)
        List<PopularLoanedBookProjection> findPopularLoanedBooksForSystem(Pageable pageable);

        @Query("""
                        select year(l.loanDate) as year,
                               month(l.loanDate) as month,
                               count(l.id) as loanCount
                        from Loan l
                        where l.campus.school.id = :schoolId
                        and (:campusId is null or l.campus.id = :campusId)
                        and l.loanDate is not null
                        group by year(l.loanDate), month(l.loanDate)
                        order by year(l.loanDate), month(l.loanDate)
                        """)
        List<LoanVolumePeriodProjection> findMonthlyLoanVolumeForSchool(
                @Param("schoolId") Long schoolId,
                @Param("campusId") Long campusId);

        @Query("""
                        select year(l.loanDate) as year,
                               month(l.loanDate) as month,
                               count(l.id) as loanCount
                        from Loan l
                        where l.loanDate is not null
                        group by year(l.loanDate), month(l.loanDate)
                        order by year(l.loanDate), month(l.loanDate)
                        """)
        List<LoanVolumePeriodProjection> findMonthlyLoanVolumeForSystem();

        @Query("""
                        select coalesce(g.name, 'Geen klas') as classTitle,
                               count(distinct u.id) as studentCount,
                               count(bc.id) as totalLoans,
                               sum(case when l.returnedDate is null then 1 else 0 end) as activeLoans
                        from Loan l
                        join l.user u
                        left join u.groups g
                        join l.copy bc
                        where l.campus.school.id = :schoolId
                        and (:campusId is null or l.campus.id = :campusId)
                        group by coalesce(g.name, 'Geen klas')
                        order by count(bc.id) desc, coalesce(g.name, 'Geen klas') asc
                        """)
        List<ClassLoanActivityProjection> findClassLoanActivityForSchool(
                @Param("schoolId") Long schoolId,
                @Param("campusId") Long campusId);

        @Query("""
                        select count(distinct u.id)
                        from User u
                        where u.school.id = :schoolId
                        and size(u.groups) > 0
                        """)
        long countStudentsWithClassForSchool(
                @Param("schoolId") Long schoolId);

        @Query("""
                        select count(distinct g.name)
                        from User u
                        join u.groups g
                        where u.school.id = :schoolId
                        """)
        long countClassesForSchool(
                @Param("schoolId") Long schoolId);

        @Query("""
                        select count(l.copy.id)
                        from Loan l
                        where l.campus.school.id = :schoolId
                        and (:campusId is null or l.campus.id = :campusId)
                        """)
        long countLoanedBookCopiesForSchool(
                @Param("schoolId") Long schoolId,
                @Param("campusId") Long campusId);

        @Query("""
                        select count(l.copy.id)
                        from Loan l
                        where l.campus.school.id = :schoolId
                        and (:campusId is null or l.campus.id = :campusId)
                        and l.returnedDate is null
                        """)
        long countActiveLoanedBookCopiesForSchool(
                @Param("schoolId") Long schoolId,
                @Param("campusId") Long campusId);

        @Query("""
                        select count(l.copy.id)
                        from Loan l
                        """)
        long countLoanedBookCopiesForSystem();

        @Query("""
                        select count(l.copy.id)
                        from Loan l
                        where l.returnedDate is null
                        """)
        long countActiveLoanedBookCopiesForSystem();

        long countByReturnedDateIsNullAndReturnDateGreaterThanEqual(LocalDate today);

        long countByReturnedDateIsNullAndReturnDateBefore(LocalDate today);

        long countByReturnedDateIsNotNull();

        long countByCampusSchoolIdAndReturnedDateIsNullAndReturnDateGreaterThanEqual(Long schoolId, LocalDate today);

        long countByCampusSchoolIdAndReturnedDateIsNullAndReturnDateBefore(Long schoolId, LocalDate today);

        long countByCampusSchoolIdAndReturnedDateIsNotNull(Long schoolId);


}
