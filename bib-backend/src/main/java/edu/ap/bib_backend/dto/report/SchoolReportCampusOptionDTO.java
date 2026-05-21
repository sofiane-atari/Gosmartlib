package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SchoolReportCampusOptionDTO(
        @JsonProperty("campusId") Long campusId,
        @JsonProperty("campusName") String campusName) {
}
