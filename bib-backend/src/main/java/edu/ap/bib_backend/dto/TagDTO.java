package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record TagDTO(
        @JsonProperty("id") Long id,

        @JsonProperty("name") @NotBlank(message = "Tagnaam is verplicht.") String name,

        @JsonProperty("description") String description,

        @JsonProperty("bookCount") Long bookCount,

        @JsonProperty("createdAt") LocalDateTime createdAt) {
}