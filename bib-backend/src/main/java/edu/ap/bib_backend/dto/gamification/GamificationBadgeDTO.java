package edu.ap.bib_backend.dto.gamification;

import java.time.LocalDateTime;

public record GamificationBadgeDTO(
        String code,
        String name,
        String description,
        String assetKey,
        String lockedAssetKey,
        String rarity,
        String themeColor,
        String category,
        int rewardPoints,
        boolean earned,
        LocalDateTime earnedAt
) {
}