package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadingListDTO(
                @JsonProperty("name") String name) {
}
