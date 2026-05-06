package edu.ap.bib_backend.dto.gamification;

import java.time.LocalDateTime;

public record GamificationNotificationDTO(
        Long id,
        String type,
        String title,
        String message,
        String assetKey,
        String themeColor,
        String rarity,
        int rewardPoints,
        LocalDateTime createdAt
) {
}