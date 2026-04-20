package edu.ap.bib_backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.ap.bib_backend.annotation.HexColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GenreDTO(
                @JsonProperty("id") Long id,

                @JsonProperty("name") @NotBlank(message = "Genrenaam mag niet leeg zijn") @NotNull(message = "Genrenaam is verplicht") String name,

                @JsonProperty("color") @HexColor(message = "Ongeldige hexkleur") @NotBlank(message = "Genrekleur mag niet leeg zijn") @NotNull(message = "Genrekleur is verplicht") String color,

                @JsonProperty("description") String description,

                @JsonProperty("bookCount") @JsonInclude(JsonInclude.Include.NON_NULL) Integer bookCount,

                @JsonProperty("subGenres") @JsonInclude(JsonInclude.Include.NON_NULL) List<GenreDTO> subGenres) {
}