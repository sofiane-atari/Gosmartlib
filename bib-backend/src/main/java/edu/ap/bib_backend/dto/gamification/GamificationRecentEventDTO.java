package edu.ap.bib_backend.dto.gamification;

import java.time.LocalDateTime;

public record GamificationRecentEventDTO(
        String type,
        int points,
        String reason,
        LocalDateTime createdAt
) {
}