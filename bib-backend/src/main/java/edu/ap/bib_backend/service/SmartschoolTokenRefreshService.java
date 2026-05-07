package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SmartschoolTokenResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class SmartschoolTokenRefreshService {
	private final TokenStorageService tokenStorageService;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;
	private final String tokenUri;

	public SmartschoolTokenRefreshService(
			TokenStorageService tokenStorageService,
			@Value("${spring.security.oauth2.client.registration.smartschool.client-id}") String clientId,
			@Value("${spring.security.oauth2.client.registration.smartschool.client-secret}") String clientSecret,
			@Value("${spring.security.oauth2.client.registration.smartschool.redirect-uri}") String redirectUri,
			@Value("${spring.security.oauth2.client.provider.smartschool.token-uri}") String tokenUri) {
		this.tokenStorageService = tokenStorageService;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
		this.tokenUri = tokenUri;
	}

	public void refreshTokens(String userId) {
		String refreshToken = tokenStorageService.getSmartschoolRefreshToken(userId);
		if (refreshToken == null) {
			throw new IllegalStateException("No valid refresh token for user " + userId);
		}

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "refresh_token");
		params.add("refresh_token", refreshToken);
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("redirect_uri", redirectUri);

		SmartschoolTokenResponse response = RestClient.builder().build()
				.post()
				.uri(tokenUri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(params)
				.retrieve()
				.body(SmartschoolTokenResponse.class);

		if (response == null || response.accessToken() == null) {
			throw new RuntimeException("Failed to refresh tokens for user " + userId);
		}

		// Replace both tokens in DB (uses existing storeSmartschoolTokens)
		tokenStorageService.storeSmartschoolTokens(userId, response.accessToken(), response.refreshToken());
	}

	// Refreshes Smartschool access token if invalid/expired, returns valid token
	public String getValidSmartschoolAccessToken(String userId) {
		String token = tokenStorageService.getSmartschoolAccessToken(userId);
		if (token == null) {
			refreshTokens(userId);
			token = tokenStorageService.getSmartschoolAccessToken(userId);
		}
		return token;
	}
}
