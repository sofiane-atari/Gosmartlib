package edu.ap.bib_backend.dto;

public record SmartschoolRefreshResult(
        String accessToken,
        String refreshToken
) {
}