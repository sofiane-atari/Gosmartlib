package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportClassOptionDTO;
import edu.ap.bib_backend.dto.report.SchoolReportDTO;
import edu.ap.bib_backend.dto.report.SystemReportDTO;
import edu.ap.bib_backend.service.report.ReportService;
import edu.ap.bib_backend.service.report.SchoolReportService;
import edu.ap.bib_backend.service.report.SystemReportService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final SchoolReportService schoolReportService;
    private final SystemReportService systemReportService;

    public ReportController(
            ReportService reportService,
            SchoolReportService schoolReportService,
            SystemReportService systemReportService) {
        this.reportService = reportService;
        this.schoolReportService = schoolReportService;
        this.systemReportService = systemReportService;
    }

    @GetMapping("/classes")
    public List<ReportClassOptionDTO> getAvailableClasses(Authentication authentication) {
        return reportService.getAvailableClasses(authentication);
    }

    @GetMapping("/class")
    public ClassReportDTO getClassReport(
            @RequestParam String classSourcedId,
            Authentication authentication) {
        return reportService.getClassReport(classSourcedId, authentication);
    }

    @GetMapping("/school")
    public SchoolReportDTO getSchoolReport(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        return schoolReportService.generateSchoolReport(authentication, schoolId, limit, offset);
    }

    @GetMapping("/system")
    public SystemReportDTO getSystemReport(Authentication authentication) {
        return systemReportService.generateSystemReport(authentication);
    }
}
