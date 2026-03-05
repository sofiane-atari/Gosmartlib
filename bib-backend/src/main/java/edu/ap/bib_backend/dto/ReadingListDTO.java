package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReadingListDTO(
        @JsonProperty("userId") Long userId,
        @JsonProperty("name") String name
) {}
