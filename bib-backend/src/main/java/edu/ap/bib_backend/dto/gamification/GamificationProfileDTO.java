package edu.ap.bib_backend.dto.gamification;

import java.util.List;

public record GamificationProfileDTO(
        int totalPoints,
        int currentLevel,
        int pointsUntilNextLevel,
        List<GamificationBadgeDTO> latestAchievements,
        List<GamificationBadgeDTO> earnedBadges,
        List<GamificationProgressDTO> badgeProgress,
        List<GamificationRecentEventDTO> recentEvents
) {
}