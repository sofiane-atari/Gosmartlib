package edu.ap.bib_backend.dto.oneroster;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OneRosterClassDTO(
                @JsonProperty("sourcedId") String sourcedId,
                @JsonProperty("title") String title,
                @JsonProperty("classCode") String classCode) {
}