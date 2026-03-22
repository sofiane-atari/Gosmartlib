package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record GenreStatsDTO(
        @JsonProperty("totalItems") int totalItems,
        @JsonProperty("unActiveGenres") int unActiveGenres) {
}