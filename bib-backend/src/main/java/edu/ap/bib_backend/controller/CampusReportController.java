package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.report.CampusReportDTO;
import edu.ap.bib_backend.service.report.CampusReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class CampusReportController {

    private final CampusReportService reportService;

    public CampusReportController(CampusReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/my-campus")
    public CampusReportDTO getMyCampusReport(Authentication authentication) {
        System.out.println(">>> CAMPUS REPORT CONTROLLER HIT - my-campus");
        System.out.println("AUTH = " + authentication);

        return reportService.generateReportForLoggedInTeacher(authentication);
    }

    @GetMapping("/all-campuses")
    public List<CampusReportDTO> getAllCampusReports(Authentication authentication) {
        System.out.println(">>> CAMPUS REPORT CONTROLLER HIT - all-campuses");
        System.out.println("AUTH = " + authentication);

        return reportService.generateAllCampusReports(authentication);
    }

    @GetMapping("/campus/{campusId}")
    public CampusReportDTO getCampusReport(
            @PathVariable Long campusId,
            Authentication authentication) {
        System.out.println(">>> CAMPUS REPORT CONTROLLER HIT - campus/" + campusId);
        System.out.println("AUTH = " + authentication);

        return reportService.generateReportForCampusId(authentication, campusId);
    }
}



