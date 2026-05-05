package edu.ap.bib_backend.dto.oneroster;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OneRosterUserDTO(
                @JsonProperty("sourcedId") String sourcedId,
                @JsonProperty("displayName") String displayName,
                @JsonProperty("givenName") String givenName,
                @JsonProperty("familyName") String familyName,
                @JsonProperty("username") String username,
                @JsonProperty("role") String role) {
}