package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SchoolDTO(
                @JsonProperty("id") Long id,

                @JsonProperty("domainName") @NotBlank(message = "Domeinnaam is verplicht.") String domainName,

                @JsonProperty("name") @NotBlank(message = "Schoolnaam is verplicht.") String name,

                @JsonProperty("defaultLoanDurationDays") Integer defaultLoanDurationDays,

                @JsonProperty("createdAt") LocalDateTime createdAt) {
}
