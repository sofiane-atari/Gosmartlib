package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportClassOptionDTO;
import edu.ap.bib_backend.service.report.ReportService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
}