package edu.ap.bib_backend.dto.report;

import java.util.List;

public record ClassReportDTO(
        String classSourcedId,
        String classTitle,
        int studentCount,
        double classAverageScore,
        double classAverageProgress,
        List<ReportRowDTO> students) {
}