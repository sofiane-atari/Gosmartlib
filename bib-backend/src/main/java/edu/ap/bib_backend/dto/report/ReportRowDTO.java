package edu.ap.bib_backend.dto.report;

public record ReportRowDTO(
        String studentSourcedId,
        String displayName,
        int totalItems,
        int completedItems,
        double progressPercentage,
        double averageScore,
        int totalTimeMinutes) {
}