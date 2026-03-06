package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record LabelDTO(
    @JsonProperty("name") @NotNull(message =  "Labelnaam is verplicht") String name,
    @JsonProperty("description") String description,
    @JsonProperty("color") String color
) {
}