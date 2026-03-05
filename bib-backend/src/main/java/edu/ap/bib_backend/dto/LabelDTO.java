package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record LabelDTO(
    @JsonProperty("name") @NotNull(message =  "Label name is required") String name,
    @JsonProperty("description") String description,
    @JsonProperty("color") String color
) {
}