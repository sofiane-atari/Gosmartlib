package edu.ap.bib_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignLibrarianRequestDto(
    @NotBlank(message = "userId is verplicht")
    String userId
) {}
