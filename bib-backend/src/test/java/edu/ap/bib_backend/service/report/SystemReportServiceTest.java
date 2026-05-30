package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.SystemReportDTO;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.projection.report.LoanVolumePeriodProjection;
import edu.ap.bib_backend.projection.report.PopularLoanedBookProjection;
import edu.ap.bib_backend.projection.report.SystemSchoolActivityProjection;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemReportServiceTest {

    private final ReportAccessService reportAccessService = new ReportAccessService();
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);

    private final SystemReportService service = new SystemReportService(
            reportAccessService,
            loanRepository,
            schoolRepository,
            userRepository,
            bookRepository,
            bookCopyRepository);

    @Test
    void generateSystemReport_returnsAggregatedSystemDataForAdmin() {
        PopularLoanedBookProjection popularBook = popularBook(1L, "Testboek", "Auteur", null, 8L);
        LoanVolumePeriodProjection volumePeriod = volumePeriod(2026, 5, 12L);
        SystemSchoolActivityProjection schoolActivity = schoolActivity(10L, "School A", 20L, 15L, 40L, 5L);

        when(loanRepository.countLoanedBookCopiesForSystem()).thenReturn(40L);
        when(loanRepository.countActiveLoanedBookCopiesForSystem()).thenReturn(5L);
        when(userRepository.countStudentsForSystem()).thenReturn(20L);
        when(userRepository.countLinkedStudentsForSystem()).thenReturn(15L);
        when(schoolRepository.count()).thenReturn(2L);
        when(userRepository.countActiveUsersForSystem()).thenReturn(18L);
        when(bookRepository.countUniqueTitlesForSystem()).thenReturn(9L);
        when(bookCopyRepository.count()).thenReturn(30L);
        when(loanRepository.findPopularLoanedBooksForSystem(any())).thenReturn(List.of(popularBook));
        when(loanRepository.findMonthlyLoanVolumeForSystem()).thenReturn(List.of(volumePeriod));
        when(userRepository.findSystemSchoolActivity()).thenReturn(List.of(schoolActivity));
        when(loanRepository.countOnTimeReturnsForSystem()).thenReturn(8L);
        when(loanRepository.countLateReturnsForSystem()).thenReturn(2L);

        SystemReportDTO result = service.generateSystemReport(adminAuthentication());

        assertThat(result.totalLoans()).isEqualTo(40L);
        assertThat(result.activeLoans()).isEqualTo(5L);
        assertThat(result.totalSchools()).isEqualTo(2L);
        assertThat(result.activeUsers()).isEqualTo(18L);
        assertThat(result.totalBooks()).isEqualTo(9L);
        assertThat(result.totalCopies()).isEqualTo(30L);
        assertThat(result.linkedUsersPercentage()).isEqualTo(75.0);
        assertThat(result.popularBooks()).hasSize(1);
        assertThat(result.popularBooks().get(0).loanCount()).isEqualTo(8L);
        assertThat(result.loanVolume()).hasSize(1);
        assertThat(result.loanVolume().get(0).period()).isEqualTo("2026-05");
        assertThat(result.schools()).hasSize(1);
        assertThat(result.schools().get(0).averageLoansPerStudent()).isEqualTo(2.0);
        assertThat(result.schools().get(0).syncStatus()).isEqualTo("ok");

        assertThat(result.returnedOnTimeLoans()).isEqualTo(8L);
        assertThat(result.returnedLateLoans()).isEqualTo(2L);
        assertThat(result.onTimePercentage()).isEqualTo(80.0);
        assertThat(result.latePercentage()).isEqualTo(20.0);
    }

    @Test
    void generateSystemReport_rejectsNonAdmin() {
        assertThatThrownBy(() -> service.generateSystemReport(teacherAuthentication()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Alleen administrators kunnen systeemrapporten bekijken");

        verifyNoInteractions(loanRepository, schoolRepository, userRepository, bookRepository, bookCopyRepository);
    }

    @Test
    void generateSystemReport_handlesZeroStudentsAndNullProjectionValues() {
        SystemSchoolActivityProjection schoolActivity = schoolActivity(10L, "School A", null, null, null, null);

        when(userRepository.countStudentsForSystem()).thenReturn(0L);
        when(userRepository.countLinkedStudentsForSystem()).thenReturn(0L);
        when(loanRepository.findPopularLoanedBooksForSystem(any())).thenReturn(List.of());
        when(loanRepository.findMonthlyLoanVolumeForSystem()).thenReturn(List.of());
        when(userRepository.findSystemSchoolActivity()).thenReturn(List.of(schoolActivity));
        when(loanRepository.countOnTimeReturnsForSystem()).thenReturn(0L);
        when(loanRepository.countLateReturnsForSystem()).thenReturn(0L);

        SystemReportDTO result = service.generateSystemReport(adminAuthentication());

        assertThat(result.linkedUsersPercentage()).isZero();
        assertThat(result.schools()).hasSize(1);
        assertThat(result.schools().get(0).totalStudents()).isZero();
        assertThat(result.schools().get(0).linkedStudents()).isZero();
        assertThat(result.schools().get(0).totalLoans()).isZero();
        assertThat(result.schools().get(0).activeLoans()).isZero();
        assertThat(result.schools().get(0).averageLoansPerStudent()).isZero();

        assertThat(result.returnedOnTimeLoans()).isZero();
        assertThat(result.returnedLateLoans()).isZero();
        assertThat(result.onTimePercentage()).isZero();
        assertThat(result.latePercentage()).isZero();
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UsernamePasswordAuthenticationToken teacherAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "teacher-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private PopularLoanedBookProjection popularBook(Long bookId, String title, String author, String imageUrl, Long loanCount) {
        PopularLoanedBookProjection projection = mock(PopularLoanedBookProjection.class);
        when(projection.getBookId()).thenReturn(bookId);
        when(projection.getTitle()).thenReturn(title);
        when(projection.getAuthor()).thenReturn(author);
        when(projection.getImageUrl()).thenReturn(imageUrl);
        when(projection.getLoanCount()).thenReturn(loanCount);
        return projection;
    }

    private LoanVolumePeriodProjection volumePeriod(Integer year, Integer month, Long loanCount) {
        LoanVolumePeriodProjection projection = mock(LoanVolumePeriodProjection.class);
        when(projection.getYear()).thenReturn(year);
        when(projection.getMonth()).thenReturn(month);
        when(projection.getLoanCount()).thenReturn(loanCount);
        return projection;
    }

    private SystemSchoolActivityProjection schoolActivity(
            Long schoolId,
            String schoolName,
            Long totalStudents,
            Long linkedStudents,
            Long totalLoans,
            Long activeLoans) {
        SystemSchoolActivityProjection projection = mock(SystemSchoolActivityProjection.class);
        when(projection.getSchoolId()).thenReturn(schoolId);
        when(projection.getSchoolName()).thenReturn(schoolName);
        when(projection.getTotalStudents()).thenReturn(totalStudents);
        when(projection.getLinkedStudents()).thenReturn(linkedStudents);
        when(projection.getTotalLoans()).thenReturn(totalLoans);
        when(projection.getActiveLoans()).thenReturn(activeLoans);
        return projection;
    }
}
