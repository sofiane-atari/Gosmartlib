package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OneRosterAudienceOptionsDto(
        @JsonProperty("classes") List<String> classes,
        @JsonProperty("years") List<String> years,
        @JsonProperty("schools") List<String> schools) {
}

