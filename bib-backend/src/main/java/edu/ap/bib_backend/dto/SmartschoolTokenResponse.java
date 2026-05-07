package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SmartschoolTokenResponse(
		@JsonProperty("token_type") String tokenType,
		@JsonProperty("expires_in") Integer expiresIn,
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("refresh_token") String refreshToken) {
}
