package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.ap.bib_backend.jpa.enums.LoanExtensionType;

public record LoanExtensionDTO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("oldReturnDate")
        String oldReturnDate,

        @JsonProperty("newReturnDate")
        String newReturnDate,

        @JsonProperty("extensionType")
        LoanExtensionType extensionType,

        @JsonProperty("extendedByUserId")
        String extendedByUserId,

        @JsonProperty("extendedAt")
        String extendedAt
) {}