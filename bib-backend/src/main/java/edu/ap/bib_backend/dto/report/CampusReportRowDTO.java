package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CampusReportRowDTO(
        @JsonProperty("classSourcedId") String classSourcedId,
        @JsonProperty("classTitle") String classTitle,

        @JsonProperty("studentCount") int studentCount,
        @JsonProperty("linkedStudentCount") int linkedStudentCount,

        @JsonProperty("totalLoans") long totalLoans,
        @JsonProperty("activeLoans") long activeLoans,
        @JsonProperty("averageLoansPerLinkedStudent") double averageLoansPerLinkedStudent,

        @JsonProperty("classAverageScore") double classAverageScore,
        @JsonProperty("classAverageProgress") double classAverageProgress) {
}

