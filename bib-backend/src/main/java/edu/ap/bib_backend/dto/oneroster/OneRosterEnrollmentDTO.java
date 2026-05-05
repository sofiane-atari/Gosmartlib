package edu.ap.bib_backend.dto.oneroster;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OneRosterEnrollmentDTO(
        @JsonProperty("sourcedId") String sourcedId,
        @JsonProperty("role") String role,
        @JsonProperty("userSourcedId") String userSourcedId,
        @JsonProperty("classSourcedId") String classSourcedId) {
}