package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.ap.bib_backend.annotation.HexColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GenreDTO(
        @JsonProperty("name") @NotBlank(message = "Genrenaam is mag niet leeg zijn") @NotNull(message = "Genrenaam is verplicht") String name,
        @JsonProperty("color") @HexColor(message = "Ongeldige hexkleur") @NotBlank(message = "Genrekleur is mag niet leeg zijn") @NotNull(message = "Genrekleur is verplicht") String color,
        @JsonProperty("description") String description,
        @JsonProperty("booksCount") @JsonInclude(JsonInclude.Include.NON_NULL) Integer bookCount // null when not requested, int value when included
) {
}