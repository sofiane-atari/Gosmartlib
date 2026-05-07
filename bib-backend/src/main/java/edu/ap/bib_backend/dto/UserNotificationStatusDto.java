package edu.ap.bib_backend.dto;

public record UserNotificationStatusDto(
        boolean hasValidRefreshToken
) {
}
