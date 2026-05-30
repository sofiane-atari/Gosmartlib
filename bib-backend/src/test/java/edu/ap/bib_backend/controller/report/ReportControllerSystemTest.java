package edu.ap.bib_backend.controller.report;

import edu.ap.bib_backend.controller.ReportController;
import edu.ap.bib_backend.dto.report.SystemReportDTO;
import edu.ap.bib_backend.service.report.ReportService;
import edu.ap.bib_backend.service.report.SchoolReportService;
import edu.ap.bib_backend.service.report.SystemReportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerSystemTest {

    private final ReportService reportService = mock(ReportService.class);
    private final SchoolReportService schoolReportService = mock(SchoolReportService.class);
    private final SystemReportService systemReportService = mock(SystemReportService.class);

    private final ReportController controller = new ReportController(
            reportService,
            schoolReportService,
            systemReportService);

    @Test
    void getSystemReport_returnsReportFromService() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin-1", null, "ROLE_ADMIN");
        SystemReportDTO report = new SystemReportDTO(
                12,
                3,
                2,
                9,
                4,
                20,
                75.0,
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                0.0,
                0.0);

        when(systemReportService.generateSystemReport(authentication)).thenReturn(report);

        SystemReportDTO result = controller.getSystemReport(authentication);

        assertThat(result).isSameAs(report);
        assertThat(result.totalLoans()).isEqualTo(12);
        verify(systemReportService).generateSystemReport(authentication);
    }
}
