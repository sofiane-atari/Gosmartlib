package edu.ap.bib_backend.controller.report;

import edu.ap.bib_backend.controller.ClassReportController;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.service.report.ClassReportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClassReportControllerTest {

    private final ClassReportService reportService = mock(ClassReportService.class);
    private final ClassReportController controller = new ClassReportController(reportService);

    @Test
    void getMyClassReports_returnsReportsFromService() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("teacher-1", null, "ROLE_TEACHER");

        ClassReportDTO report = new ClassReportDTO(
                "class-1",
                "2ITSOF1",
                5,
                3,
                10,
                2,
                3.33,
                0,
                0,
                List.of(),
                List.of(),
                List.of());

        when(reportService.generateReportsForLoggedInTeacher(authentication))
                .thenReturn(List.of(report));

        List<ClassReportDTO> result = controller.getMyClassReports(authentication);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).classSourcedId()).isEqualTo("class-1");
        assertThat(result.get(0).classTitle()).isEqualTo("2ITSOF1");

        verify(reportService).generateReportsForLoggedInTeacher(authentication);
    }
}