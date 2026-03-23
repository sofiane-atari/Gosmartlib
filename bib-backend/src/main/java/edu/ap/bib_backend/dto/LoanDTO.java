package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoanDTO(@JsonProperty("userId") @NotBlank(message = "Gebruiker ID is verplicht") String userId,
			@JsonProperty("booksIds") @NotBlank(message = "Minstens één boek is verplicht") int[] booksIds,
			@JsonProperty("returnDate") @NotBlank(message = "Retourdatum is verplicht") String returnDate) {
}