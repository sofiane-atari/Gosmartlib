package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.PopularGenreDTO;
import edu.ap.bib_backend.dto.report.ReportRowDTO;
import edu.ap.bib_backend.dto.report.TopRatedBookReportDTO;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.statistic.StudentClassStatistic;
import edu.ap.bib_backend.jpa.repository.StudentClassStatisticRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportBuilderServiceTest {

    private final StudentClassStatisticRepository statisticRepository = mock(StudentClassStatisticRepository.class);

    private final UserRepository userRepository = mock(UserRepository.class);

    private final ReportStatisticsQueryService statisticsQueryService = mock(ReportStatisticsQueryService.class);

    private final ReportBuilderService service = new ReportBuilderService(
            statisticRepository,
            userRepository,
            statisticsQueryService);

    @Test
    void buildReport_buildsClassReportWithStudentsStatisticsAndLibraryData() {
        OneRosterClassDTO oneRosterClass = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        OneRosterUserDTO studentOne = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        OneRosterUserDTO studentTwo = new OneRosterUserDTO(
                "student-2",
                "Anna Janssens",
                "Anna",
                "Janssens",
                "anna.janssens",
                "student");

        StudentClassStatistic statisticOne = new StudentClassStatistic();
        statisticOne.setStudentSourcedId("student-1");
        statisticOne.setClassSourcedId("class-1");
        statisticOne.setTotalItems(10);
        statisticOne.setCompletedItems(5);
        statisticOne.setAverageScore(8.456);
        statisticOne.setTotalTimeMinutes(120);

        StudentClassStatistic statisticTwo = new StudentClassStatistic();
        statisticTwo.setStudentSourcedId("student-2");
        statisticTwo.setClassSourcedId("class-1");
        statisticTwo.setTotalItems(4);
        statisticTwo.setCompletedItems(4);
        statisticTwo.setAverageScore(6.111);
        statisticTwo.setTotalTimeMinutes(60);

        when(statisticRepository.findByStudentSourcedIdAndClassSourcedId("student-1", "class-1"))
                .thenReturn(Optional.of(statisticOne));

        when(statisticRepository.findByStudentSourcedIdAndClassSourcedId("student-2", "class-1"))
                .thenReturn(Optional.of(statisticTwo));

        User userOne = mock(User.class);
        when(userOne.getId()).thenReturn("user-1");

        User userTwo = mock(User.class);
        when(userTwo.getId()).thenReturn("user-2");

        when(userRepository.findByOneRosterSourcedId("student-1"))
                .thenReturn(Optional.of(userOne));

        when(userRepository.findByOneRosterSourcedId("student-2"))
                .thenReturn(Optional.of(userTwo));

        when(statisticsQueryService.countLoanedBooksForUser("user-1")).thenReturn(3L);
        when(statisticsQueryService.countActiveLoanedBooksForUser("user-1")).thenReturn(1L);
        when(statisticsQueryService.countReturnedLoanedBooksForUser("user-1")).thenReturn(2L);
        when(statisticsQueryService.countReviewsForUser("user-1")).thenReturn(1L);

        when(statisticsQueryService.countLoanedBooksForUser("user-2")).thenReturn(7L);
        when(statisticsQueryService.countActiveLoanedBooksForUser("user-2")).thenReturn(2L);
        when(statisticsQueryService.countReturnedLoanedBooksForUser("user-2")).thenReturn(5L);
        when(statisticsQueryService.countReviewsForUser("user-2")).thenReturn(4L);

        PopularGenreDTO genre = new PopularGenreDTO(1L, "Fantasy", "#7c3aed", 5L);

        TopRatedBookReportDTO book = new TopRatedBookReportDTO(10L, "Testboek", "Auteur", "image.jpg", 4.5, 2L);

        when(statisticsQueryService.findPopularGenresForUsers(List.of("user-2", "user-1")))
                .thenReturn(List.of(genre));

        when(statisticsQueryService.findTopRatedBooksForUsers(List.of("user-2", "user-1")))
                .thenReturn(List.of(book));

        ClassReportDTO result = service.buildReport(
                oneRosterClass,
                List.of(studentOne, studentTwo));

        assertThat(result.classSourcedId()).isEqualTo("class-1");
        assertThat(result.classTitle()).isEqualTo("2ITSOF1");
        assertThat(result.studentCount()).isEqualTo(2);
        assertThat(result.linkedStudentCount()).isEqualTo(2);
        assertThat(result.totalLoans()).isEqualTo(10);
        assertThat(result.activeLoans()).isEqualTo(3);
        assertThat(result.averageLoansPerLinkedStudent()).isEqualTo(5.0);
        assertThat(result.classAverageScore()).isEqualTo(7.29);
        assertThat(result.classAverageProgress()).isEqualTo(75.0);

        assertThat(result.popularGenres()).containsExactly(genre);
        assertThat(result.topRatedBooks()).containsExactly(book);

        assertThat(result.students()).hasSize(2);

        assertThat(result.students().get(0).studentSourcedId()).isEqualTo("student-2");
        assertThat(result.students().get(0).displayName()).isEqualTo("Anna Janssens");
        assertThat(result.students().get(0).linkedUserId()).isEqualTo("user-2");
        assertThat(result.students().get(0).totalLoans()).isEqualTo(7);
        assertThat(result.students().get(0).activeLoans()).isEqualTo(2);
        assertThat(result.students().get(0).returnedLoans()).isEqualTo(5);
        assertThat(result.students().get(0).reviewCount()).isEqualTo(4);
        assertThat(result.students().get(0).progressPercentage()).isEqualTo(100.0);
        assertThat(result.students().get(0).averageScore()).isEqualTo(6.11);

        assertThat(result.students().get(1).studentSourcedId()).isEqualTo("student-1");
        assertThat(result.students().get(1).displayName()).isEqualTo("Jan Peeters");
        assertThat(result.students().get(1).linkedUserId()).isEqualTo("user-1");
        assertThat(result.students().get(1).totalLoans()).isEqualTo(3);
        assertThat(result.students().get(1).activeLoans()).isEqualTo(1);
        assertThat(result.students().get(1).returnedLoans()).isEqualTo(2);
        assertThat(result.students().get(1).reviewCount()).isEqualTo(1);
        assertThat(result.students().get(1).progressPercentage()).isEqualTo(50.0);
        assertThat(result.students().get(1).averageScore()).isEqualTo(8.46);
    }

    @Test
    void buildReport_handlesStudentWithoutStatistic() {
        OneRosterClassDTO oneRosterClass = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        OneRosterUserDTO student = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        User user = mock(User.class);
        when(user.getId()).thenReturn("user-1");

        when(statisticRepository.findByStudentSourcedIdAndClassSourcedId("student-1", "class-1"))
                .thenReturn(Optional.empty());

        when(userRepository.findByOneRosterSourcedId("student-1"))
                .thenReturn(Optional.of(user));

        when(statisticsQueryService.countLoanedBooksForUser("user-1")).thenReturn(2L);
        when(statisticsQueryService.countActiveLoanedBooksForUser("user-1")).thenReturn(1L);
        when(statisticsQueryService.countReturnedLoanedBooksForUser("user-1")).thenReturn(1L);
        when(statisticsQueryService.countReviewsForUser("user-1")).thenReturn(3L);

        when(statisticsQueryService.findPopularGenresForUsers(List.of("user-1")))
                .thenReturn(List.of());

        when(statisticsQueryService.findTopRatedBooksForUsers(List.of("user-1")))
                .thenReturn(List.of());

        ClassReportDTO result = service.buildReport(
                oneRosterClass,
                List.of(student));

        assertThat(result.studentCount()).isEqualTo(1);
        assertThat(result.linkedStudentCount()).isEqualTo(1);
        assertThat(result.totalLoans()).isEqualTo(2);
        assertThat(result.activeLoans()).isEqualTo(1);
        assertThat(result.averageLoansPerLinkedStudent()).isEqualTo(2.0);
        assertThat(result.classAverageScore()).isEqualTo(0.0);
        assertThat(result.classAverageProgress()).isEqualTo(0.0);

        assertThat(result.students()).hasSize(1);

        ReportRowDTO row = result.students().get(0);

        assertThat(row.studentSourcedId()).isEqualTo("student-1");
        assertThat(row.displayName()).isEqualTo("Jan Peeters");
        assertThat(row.totalItems()).isEqualTo(0);
        assertThat(row.completedItems()).isEqualTo(0);
        assertThat(row.progressPercentage()).isEqualTo(0);
        assertThat(row.averageScore()).isEqualTo(0);
        assertThat(row.totalTimeMinutes()).isEqualTo(0);
        assertThat(row.linkedUserId()).isEqualTo("user-1");
        assertThat(row.totalLoans()).isEqualTo(2);
        assertThat(row.activeLoans()).isEqualTo(1);
        assertThat(row.returnedLoans()).isEqualTo(1);
        assertThat(row.reviewCount()).isEqualTo(3);
    }

    @Test
    void buildReport_handlesUnlinkedStudent() {
        OneRosterClassDTO oneRosterClass = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        OneRosterUserDTO student = new OneRosterUserDTO(
                "student-1",
                "Niet Gekoppeld",
                "Niet",
                "Gekoppeld",
                "niet.gekoppeld",
                "student");

        when(statisticRepository.findByStudentSourcedIdAndClassSourcedId("student-1", "class-1"))
                .thenReturn(Optional.empty());

        when(userRepository.findByOneRosterSourcedId("student-1"))
                .thenReturn(Optional.empty());

        when(statisticsQueryService.findPopularGenresForUsers(List.of()))
                .thenReturn(List.of());

        when(statisticsQueryService.findTopRatedBooksForUsers(List.of()))
                .thenReturn(List.of());

        ClassReportDTO result = service.buildReport(
                oneRosterClass,
                List.of(student));

        assertThat(result.studentCount()).isEqualTo(1);
        assertThat(result.linkedStudentCount()).isEqualTo(0);
        assertThat(result.totalLoans()).isEqualTo(0);
        assertThat(result.activeLoans()).isEqualTo(0);
        assertThat(result.averageLoansPerLinkedStudent()).isEqualTo(0.0);

        ReportRowDTO row = result.students().get(0);

        assertThat(row.studentSourcedId()).isEqualTo("student-1");
        assertThat(row.linkedUserId()).isNull();
        assertThat(row.totalLoans()).isEqualTo(0);
        assertThat(row.activeLoans()).isEqualTo(0);
        assertThat(row.returnedLoans()).isEqualTo(0);
        assertThat(row.reviewCount()).isEqualTo(0);

        verify(statisticsQueryService, never()).countLoanedBooksForUser(anyString());
        verify(statisticsQueryService, never()).countActiveLoanedBooksForUser(anyString());
        verify(statisticsQueryService, never()).countReturnedLoanedBooksForUser(anyString());
        verify(statisticsQueryService, never()).countReviewsForUser(anyString());
    }

    @Test
    void buildReport_handlesEmptyStudentList() {
        OneRosterClassDTO oneRosterClass = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        when(statisticsQueryService.findPopularGenresForUsers(List.of()))
                .thenReturn(List.of());

        when(statisticsQueryService.findTopRatedBooksForUsers(List.of()))
                .thenReturn(List.of());

        ClassReportDTO result = service.buildReport(
                oneRosterClass,
                List.of());

        assertThat(result.classSourcedId()).isEqualTo("class-1");
        assertThat(result.classTitle()).isEqualTo("2ITSOF1");
        assertThat(result.studentCount()).isEqualTo(0);
        assertThat(result.linkedStudentCount()).isEqualTo(0);
        assertThat(result.totalLoans()).isEqualTo(0);
        assertThat(result.activeLoans()).isEqualTo(0);
        assertThat(result.averageLoansPerLinkedStudent()).isEqualTo(0.0);
        assertThat(result.classAverageScore()).isEqualTo(0.0);
        assertThat(result.classAverageProgress()).isEqualTo(0.0);
        assertThat(result.popularGenres()).isEmpty();
        assertThat(result.topRatedBooks()).isEmpty();
        assertThat(result.students()).isEmpty();

        verifyNoInteractions(statisticRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void buildReport_calculatesZeroProgressWhenTotalItemsIsZero() {
        OneRosterClassDTO oneRosterClass = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        OneRosterUserDTO student = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        StudentClassStatistic statistic = new StudentClassStatistic();
        statistic.setStudentSourcedId("student-1");
        statistic.setClassSourcedId("class-1");
        statistic.setTotalItems(0);
        statistic.setCompletedItems(0);
        statistic.setAverageScore(7.0);
        statistic.setTotalTimeMinutes(15);

        when(statisticRepository.findByStudentSourcedIdAndClassSourcedId("student-1", "class-1"))
                .thenReturn(Optional.of(statistic));

        when(userRepository.findByOneRosterSourcedId("student-1"))
                .thenReturn(Optional.empty());

        when(statisticsQueryService.findPopularGenresForUsers(List.of()))
                .thenReturn(List.of());

        when(statisticsQueryService.findTopRatedBooksForUsers(List.of()))
                .thenReturn(List.of());

        ClassReportDTO result = service.buildReport(
                oneRosterClass,
                List.of(student));

        ReportRowDTO row = result.students().get(0);

        assertThat(row.totalItems()).isEqualTo(0);
        assertThat(row.completedItems()).isEqualTo(0);
        assertThat(row.progressPercentage()).isEqualTo(0.0);
        assertThat(row.averageScore()).isEqualTo(7.0);
        assertThat(row.totalTimeMinutes()).isEqualTo(15);
    }
}