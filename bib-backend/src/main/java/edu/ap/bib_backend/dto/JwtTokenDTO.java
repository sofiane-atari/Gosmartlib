package edu.ap.bib_backend.dto;

import java.time.Instant;

public record JwtTokenDTO(
        String token,
        Instant expiresAt) {
}