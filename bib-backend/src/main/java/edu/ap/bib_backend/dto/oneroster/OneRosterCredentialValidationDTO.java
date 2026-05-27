package edu.ap.bib_backend.dto.oneroster;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record OneRosterCredentialValidationDTO(
        @JsonProperty("domainName") @NotBlank(message = "Domeinnaam is verplicht.") String domainName,
        @JsonProperty("clientId") @NotBlank(message = "Client-ID is verplicht.") String clientId,
        @JsonProperty("clientSecret") @NotBlank(message = "Client-geheim is verplicht.") String clientSecret
) {}
