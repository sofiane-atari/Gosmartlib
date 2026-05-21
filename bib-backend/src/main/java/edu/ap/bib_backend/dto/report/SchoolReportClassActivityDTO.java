package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SchoolReportClassActivityDTO(
        @JsonProperty("classTitle") String classTitle,
        @JsonProperty("studentCount") long studentCount,
        @JsonProperty("totalLoans") long totalLoans,
        @JsonProperty("activeLoans") long activeLoans,
        @JsonProperty("averageLoansPerStudent") double averageLoansPerStudent) {
}
