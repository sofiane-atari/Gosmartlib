package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ClassReportServiceTest {

    private final OneRosterClient oneRosterClient = mock(OneRosterClient.class);
    private final ReportAccessService reportAccessService = mock(ReportAccessService.class);
    private final ReportContextService reportContextService = mock(ReportContextService.class);
    private final ReportClassMatcherService classMatcherService = mock(ReportClassMatcherService.class);
    private final ReportStudentResolverService studentResolverService = mock(ReportStudentResolverService.class);
    private final ReportBuilderService reportBuilderService = mock(ReportBuilderService.class);

    private final ClassReportService service = new ClassReportService(
            oneRosterClient,
            reportAccessService,
            reportContextService,
            classMatcherService,
            studentResolverService,
            reportBuilderService);

    @Test
    void generateReportsForLoggedInTeacher_delegeertAuthenticationCheck() {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Je moet ingelogd zijn om klasrapporten te bekijken");

        doThrow(exception)
                .when(reportAccessService)
                .requireAuthenticated(null);

        assertThatThrownBy(() -> service.generateReportsForLoggedInTeacher(null))
                .isSameAs(exception);

        verify(reportAccessService).requireAuthenticated(null);
        verifyNoInteractions(reportContextService);
        verifyNoInteractions(oneRosterClient);
        verifyNoInteractions(classMatcherService);
        verifyNoInteractions(studentResolverService);
        verifyNoInteractions(reportBuilderService);
    }

    @Test
    void generateReportsForLoggedInTeacher_delegeertRoleCheck() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user-1", null, "ROLE_STUDENT");
        authentication.setAuthenticated(true);

        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Alleen leerkrachten kunnen klasrapporten bekijken");

        doThrow(exception)
                .when(reportAccessService)
                .requireReportAccess(authentication);

        assertThatThrownBy(() -> service.generateReportsForLoggedInTeacher(authentication))
                .isSameAs(exception);

        verify(reportAccessService).requireAuthenticated(authentication);
        verify(reportAccessService).requireReportAccess(authentication);
        verifyNoInteractions(reportContextService);
        verifyNoInteractions(oneRosterClient);
        verifyNoInteractions(classMatcherService);
        verifyNoInteractions(studentResolverService);
        verifyNoInteractions(reportBuilderService);
    }

    @Test
    void generateReportsForLoggedInTeacher_buildsReportsForMatchedClasses() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("teacher-1", null, "ROLE_TEACHER");
        authentication.setAuthenticated(true);

        ReportContextService.ReportContext context = new ReportContextService.ReportContext(
                "smartschool-token",
                "https://aphogeschool.smartschool.be",
                "aphogeschool.smartschool.be");

        OneRosterClassDTO classOne = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        OneRosterClassDTO classTwo = new OneRosterClassDTO("class-2", "2ITSOF2", "2ITSOF2");

        OneRosterUserDTO studentOne = new OneRosterUserDTO("student-1", "Jan Peeters", "Jan", "Peeters", "jan.peeters",
                "student");

        OneRosterUserDTO studentTwo = new OneRosterUserDTO("student-2", "Anna Janssens", "Anna", "Janssens",
                "anna.janssens", "student");

        ClassReportDTO reportOne = mock(ClassReportDTO.class);
        ClassReportDTO reportTwo = mock(ClassReportDTO.class);

        when(reportContextService.getContext(authentication))
                .thenReturn(context);

        when(oneRosterClient.fetchClassesWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(classOne, classTwo));

        when(reportContextService.getSmartschoolGroupNames(
                "smartschool-token",
                "https://aphogeschool.smartschool.be")).thenReturn(List.of("2ITSOF1", "2ITSOF2"));

        when(classMatcherService.findMatches(
                List.of("2ITSOF1", "2ITSOF2"),
                List.of(classOne, classTwo))).thenReturn(List.of(classOne, classTwo));

        when(studentResolverService.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1")).thenReturn(List.of(studentOne));

        when(studentResolverService.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-2")).thenReturn(List.of(studentTwo));

        when(reportBuilderService.buildReport(classOne, List.of(studentOne)))
                .thenReturn(reportOne);

        when(reportBuilderService.buildReport(classTwo, List.of(studentTwo)))
                .thenReturn(reportTwo);

        List<ClassReportDTO> result = service.generateReportsForLoggedInTeacher(authentication);

        assertThat(result).containsExactly(reportOne, reportTwo);

        verify(reportAccessService).requireAuthenticated(authentication);
        verify(reportAccessService).requireReportAccess(authentication);

        verify(reportContextService).getContext(authentication);

        verify(oneRosterClient).fetchClassesWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token");

        verify(reportContextService).getSmartschoolGroupNames(
                "smartschool-token",
                "https://aphogeschool.smartschool.be");

        verify(classMatcherService).findMatches(
                List.of("2ITSOF1", "2ITSOF2"),
                List.of(classOne, classTwo));

        verify(studentResolverService).resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        verify(studentResolverService).resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-2");

        verify(reportBuilderService).buildReport(classOne, List.of(studentOne));
        verify(reportBuilderService).buildReport(classTwo, List.of(studentTwo));
    }

    @Test
    void generateReportsForLoggedInTeacher_returnsEmptyList_whenNoClassMatches() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("teacher-1", null, "ROLE_TEACHER");
        authentication.setAuthenticated(true);

        ReportContextService.ReportContext context = new ReportContextService.ReportContext(
                "smartschool-token",
                "https://aphogeschool.smartschool.be",
                "aphogeschool.smartschool.be");

        OneRosterClassDTO oneRosterClass = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

        when(reportContextService.getContext(authentication))
                .thenReturn(context);

        when(oneRosterClient.fetchClassesWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(oneRosterClass));

        when(reportContextService.getSmartschoolGroupNames(
                "smartschool-token",
                "https://aphogeschool.smartschool.be")).thenReturn(List.of("UNKNOWN"));

        when(classMatcherService.findMatches(
                List.of("UNKNOWN"),
                List.of(oneRosterClass))).thenReturn(List.of());

        List<ClassReportDTO> result = service.generateReportsForLoggedInTeacher(authentication);

        assertThat(result).isEmpty();

        verify(reportAccessService).requireAuthenticated(authentication);
        verify(reportAccessService).requireReportAccess(authentication);

        verify(reportContextService).getContext(authentication);

        verify(oneRosterClient).fetchClassesWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token");

        verify(reportContextService).getSmartschoolGroupNames(
                "smartschool-token",
                "https://aphogeschool.smartschool.be");

        verify(classMatcherService).findMatches(
                List.of("UNKNOWN"),
                List.of(oneRosterClass));

        verifyNoInteractions(studentResolverService);
        verifyNoInteractions(reportBuilderService);
    }
}