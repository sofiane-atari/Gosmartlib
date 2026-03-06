package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.ap.bib_backend.annotation.HexColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LabelDTO(
    @JsonProperty("name") @NotBlank(message = "Labelnaam is mag niet leeg zijn") @NotNull(message =  "Labelnaam is verplicht") String name,
    @JsonProperty("color") @HexColor(message = "Ongeldige hexkleur") @NotBlank(message = "Labelkleur is mag niet leeg zijn") @NotNull(message = "Labelkleur is verplicht") String color,
    @JsonProperty("description") String description
) {
}