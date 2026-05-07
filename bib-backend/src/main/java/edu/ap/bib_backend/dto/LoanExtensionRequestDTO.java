package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.ap.bib_backend.jpa.enums.LoanExtensionType;
import jakarta.validation.constraints.NotNull;

public record LoanExtensionRequestDTO(
        @JsonProperty("extensionType")
        @NotNull(message = "Type verlenging is verplicht")
        LoanExtensionType extensionType,

        @JsonProperty("customReturnDate")
        String customReturnDate
) {}