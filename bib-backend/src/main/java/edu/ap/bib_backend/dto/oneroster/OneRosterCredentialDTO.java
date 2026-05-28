package edu.ap.bib_backend.dto.oneroster;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record OneRosterCredentialDTO(
        @JsonProperty("clientId") @NotBlank(message = "Client ID is verplicht.") String clientId,
        @JsonProperty("clientSecret") @NotBlank(message = "Client secret is verplicht.") String clientSecret
) {}
