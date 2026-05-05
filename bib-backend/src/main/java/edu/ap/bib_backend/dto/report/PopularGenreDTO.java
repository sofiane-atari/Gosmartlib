package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PopularGenreDTO(
        @JsonProperty("genreId") Long genreId,
        @JsonProperty("name") String name,
        @JsonProperty("color") String color,
        @JsonProperty("loanCount") long loanCount) {
}