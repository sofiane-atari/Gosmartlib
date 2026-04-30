package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SchoolDTO(
                @JsonProperty("id") Long id,

                @JsonProperty("domainName") @NotBlank(message = "Domeinnaam is verplicht.") String domainName,

                @JsonProperty("name") @NotBlank(message = "Schoolnaam is verplicht.") String name,

                @JsonProperty("address") @NotBlank(message = "Adres is verplicht.") String address,

                @JsonProperty("postalCode") @NotNull(message = "Postcode is verplicht.") @Min(value = 1, message = "Postcode moet groter zijn dan 0.") Integer postalCode,

                @JsonProperty("contactPhone") String contactPhone,

                @JsonProperty("contactEmail") @Email(message = "Vul een geldig e-mailadres in.") String contactEmail,

                @JsonProperty("createdAt") LocalDateTime createdAt) {
}