package edu.ap.bib_backend.dto.gamification;

public record GamificationProgressDTO(
        String badgeCode,
        String badgeName,
        String badgeDescription,
        String assetKey,
        String lockedAssetKey,
        String rarity,
        String themeColor,
        String category,
        int currentValue,
        int targetValue,
        int progressPercentage,
        boolean completed
) {
}