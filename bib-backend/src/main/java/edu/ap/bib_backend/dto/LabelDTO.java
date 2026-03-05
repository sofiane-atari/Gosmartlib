package edu.ap.bib_backend.dto;

import jakarta.validation.constraints.NotNull;

public record LabelDTO(
    @NotNull String name,
    String description,
    String color
) {
}