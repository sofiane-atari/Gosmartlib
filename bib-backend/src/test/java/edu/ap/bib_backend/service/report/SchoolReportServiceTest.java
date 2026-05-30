package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.SchoolReportDTO;
import edu.ap.bib_backend.dto.report.PopularGenreDTO;
import edu.ap.bib_backend.dto.report.TopRatedBookReportDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.projection.report.ClassLoanActivityProjection;
import edu.ap.bib_backend.projection.report.LoanVolumePeriodProjection;
import edu.ap.bib_backend.projection.report.PopularLoanedBookProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolReportServiceTest {

    private final ReportAccessService reportAccessService = new ReportAccessService();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final ReportStatisticsQueryService statisticsQueryService = mock(ReportStatisticsQueryService.class);

    private final SchoolReportService service = new SchoolReportService(
            reportAccessService,
            userRepository,
            schoolRepository,
            loanRepository,
            statisticsQueryService);

    @Test
    void generateSchoolReport_aggregatesSchoolLevelDataAndCalculatesAverage() {
        User user = user("teacher-1", 1L, "School Een");
        when(userRepository.findById("teacher-1")).thenReturn(Optional.of(user));

        PopularLoanedBookProjection popularBook = popularBook(10L, "Boek A", null, null, 7L);
        LoanVolumePeriodProjection volumePeriod = volumePeriod(2026, 5, 12L);
        ClassLoanActivityProjection classActivity = classActivity("2ITSOF1", 5L, 12L, 3L);

        when(loanRepository.findPopularLoanedBooksForSchool(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(popularBook));
        when(loanRepository.findMonthlyLoanVolumeForSchool(1L, null)).thenReturn(List.of(volumePeriod));
        when(loanRepository.findClassLoanActivityForSchool(1L, null)).thenReturn(List.of(classActivity));
        when(loanRepository.countLoanedBookCopiesForSchool(1L, null)).thenReturn(12L);
        when(loanRepository.countActiveLoanedBookCopiesForSchool(1L, null)).thenReturn(3L);
        when(loanRepository.countClassesForSchool(1L, null)).thenReturn(4L);
        when(loanRepository.countStudentsWithClassForSchool(1L, null)).thenReturn(5L);
        when(loanRepository.countOnTimeReturnsForSchool(1L, null)).thenReturn(8L);
        when(loanRepository.countLateReturnsForSchool(1L, null)).thenReturn(4L);
        when(statisticsQueryService.findPopularGenresForSchool(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(new PopularGenreDTO(1L, "Fantasy", "#123456", 9L)));
        when(statisticsQueryService.findTopRatedBooksForSchool(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(new TopRatedBookReportDTO(20L, "Boek B", "Auteur", null, 4.75, 3L)));

        SchoolReportDTO result = service.generateSchoolReport(teacherAuthentication("teacher-1"), null, 50, 35);

        assertThat(result.schoolId()).isEqualTo(1L);
        assertThat(result.schoolName()).isEqualTo("School Een");
        assertThat(result.totalLoans()).isEqualTo(12L);
        assertThat(result.activeLoans()).isEqualTo(3L);
        assertThat(result.classCount()).isEqualTo(4);
        assertThat(result.totalStudentCount()).isEqualTo(5L);
        assertThat(result.loansPerStudent()).isEqualTo(2.4);

        assertThat(result.popularBooks()).hasSize(1);
        assertThat(result.popularBooks().get(0).bookId()).isEqualTo(10L);
        assertThat(result.popularBooks().get(0).loanCount()).isEqualTo(7L);

        assertThat(result.loanVolume()).hasSize(1);
        assertThat(result.loanVolume().get(0).period()).isEqualTo("2026-05");
        assertThat(result.loanVolume().get(0).loanCount()).isEqualTo(12L);

        assertThat(result.classActivity()).hasSize(1);
        assertThat(result.classActivity().get(0).averageLoansPerStudent()).isEqualTo(2.4);
        assertThat(result.popularGenres()).hasSize(1);
        assertThat(result.popularGenres().get(0).name()).isEqualTo("Fantasy");
        assertThat(result.topRatedBooks()).hasSize(1);
        assertThat(result.topRatedBooks().get(0).averageRating()).isEqualTo(4.75);

        assertThat(result.returnedOnTimeLoans()).isEqualTo(8L);
        assertThat(result.returnedLateLoans()).isEqualTo(4L);
        assertThat(result.onTimePercentage()).isEqualTo(66.67);
        assertThat(result.latePercentage()).isEqualTo(33.33);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(loanRepository).findPopularLoanedBooksForSchool(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getOffset()).isEqualTo(35);

        ArgumentCaptor<Pageable> genrePageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(statisticsQueryService).findPopularGenresForSchool(any(), any(), genrePageableCaptor.capture());
        assertThat(genrePageableCaptor.getValue().getPageSize()).isEqualTo(5);

        ArgumentCaptor<Pageable> topRatedPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(statisticsQueryService).findTopRatedBooksForSchool(any(), any(), topRatedPageableCaptor.capture());
        assertThat(topRatedPageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void generateSchoolReport_returnsZeroAverageWhenSchoolHasNoStudents() {
        User user = user("teacher-1", 1L, "School Een");
        when(userRepository.findById("teacher-1")).thenReturn(Optional.of(user));

        when(loanRepository.findPopularLoanedBooksForSchool(any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(loanRepository.findMonthlyLoanVolumeForSchool(1L, null)).thenReturn(List.of());
        when(loanRepository.findClassLoanActivityForSchool(1L, null)).thenReturn(List.of());
        when(loanRepository.countLoanedBookCopiesForSchool(1L, null)).thenReturn(8L);
        when(loanRepository.countActiveLoanedBookCopiesForSchool(1L, null)).thenReturn(0L);
        when(loanRepository.countClassesForSchool(1L, null)).thenReturn(0L);
        when(loanRepository.countStudentsWithClassForSchool(1L, null)).thenReturn(0L);
        when(loanRepository.countOnTimeReturnsForSchool(1L, null)).thenReturn(0L);
        when(loanRepository.countLateReturnsForSchool(1L, null)).thenReturn(0L);

        SchoolReportDTO result = service.generateSchoolReport(teacherAuthentication("teacher-1"), null, 10, 0);

        assertThat(result.loansPerStudent()).isZero();
        assertThat(result.totalStudentCount()).isZero();
        assertThat(result.returnedOnTimeLoans()).isZero();
        assertThat(result.returnedLateLoans()).isZero();
        assertThat(result.onTimePercentage()).isZero();
        assertThat(result.latePercentage()).isZero();
    }

    @Test
    void generateSchoolReport_allowsAdminToRequestAnotherSchool() {
        User user = user("admin-1", 1L, "School Een");
        School requestedSchool = school(2L, "School Twee");

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(user));
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(requestedSchool));
        when(loanRepository.findPopularLoanedBooksForSchool(any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(loanRepository.findMonthlyLoanVolumeForSchool(2L, null)).thenReturn(List.of());
        when(loanRepository.findClassLoanActivityForSchool(2L, null)).thenReturn(List.of());
        when(loanRepository.countOnTimeReturnsForSchool(2L, null)).thenReturn(0L);
        when(loanRepository.countLateReturnsForSchool(2L, null)).thenReturn(0L);

        SchoolReportDTO result = service.generateSchoolReport(adminAuthentication("admin-1"), 2L, 10, 0);

        assertThat(result.schoolId()).isEqualTo(2L);
        assertThat(result.schoolName()).isEqualTo("School Twee");
    }

    @Test
    void generateSchoolReport_rejectsTeacherRequestingAnotherSchool() {
        User user = user("teacher-1", 1L, "School Een");
        when(userRepository.findById("teacher-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.generateSchoolReport(teacherAuthentication("teacher-1"), 2L, 10, 0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Je hebt geen toegang tot rapporten van deze school");

        verifyNoInteractions(schoolRepository, loanRepository);
    }

    private UsernamePasswordAuthenticationToken teacherAuthentication(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private UsernamePasswordAuthenticationToken adminAuthentication(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private User user(String userId, Long schoolId, String schoolName) {
        User user = new User();
        user.setId(userId);
        user.setSchool(school(schoolId, schoolName));
        return user;
    }

    private School school(Long id, String name) {
        School school = new School();
        school.setId(id);
        school.setName(name);
        return school;
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

    private ClassLoanActivityProjection classActivity(String classTitle, Long studentCount, Long totalLoans, Long activeLoans) {
        ClassLoanActivityProjection projection = mock(ClassLoanActivityProjection.class);
        when(projection.getClassTitle()).thenReturn(classTitle);
        when(projection.getStudentCount()).thenReturn(studentCount);
        when(projection.getTotalLoans()).thenReturn(totalLoans);
        when(projection.getActiveLoans()).thenReturn(activeLoans);
        return projection;
    }
}
