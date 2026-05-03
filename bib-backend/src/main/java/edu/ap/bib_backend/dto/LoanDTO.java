package edu.ap.bib_backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record LoanDTO(
        @JsonProperty("id") Long id,

        @JsonProperty("userId")
        @NotBlank(message = "Gebruiker ID is verplicht")
        String userId,

        @JsonProperty("bookIds")
        @NotEmpty(message = "Minstens één boek is verplicht")
        List<Long> bookIds,

        @JsonProperty("books")
        List<LoanBookDTO> books,

        @JsonProperty("loanDate")
        String loanDate,

        @JsonProperty("returnDate")
        @NotBlank(message = "Retourdatum is verplicht")
        String returnDate
) {}