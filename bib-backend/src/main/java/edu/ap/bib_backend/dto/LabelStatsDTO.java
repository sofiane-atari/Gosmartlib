package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LabelStatsDTO(
        @JsonProperty("totalItems") int totalItems,
        @JsonProperty("unActiveLabels") int unActiveLabels) {
}