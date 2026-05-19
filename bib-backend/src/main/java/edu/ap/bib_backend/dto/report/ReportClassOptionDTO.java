package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportClassOptionDTO(
        @JsonProperty("sourcedId") String sourcedId,
        @JsonProperty("title") String title,
        @JsonProperty("classCode") String classCode
) {
}