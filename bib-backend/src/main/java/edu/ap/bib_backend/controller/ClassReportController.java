package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.service.statistic.ClassReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ClassReportController {

    private final ClassReportService reportService;

    public ClassReportController(ClassReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/my-classes")
    public List<ClassReportDTO> getMyClassReports(Authentication authentication) {
        System.out.println(">>> REPORT CONTROLLER HIT");
        System.out.println("AUTH = " + authentication);

        return reportService.generateReportsForLoggedInTeacher(authentication);
    }
}