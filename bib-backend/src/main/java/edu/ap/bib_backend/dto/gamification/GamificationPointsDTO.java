package edu.ap.bib_backend.dto.gamification;

public record GamificationPointsDTO(
        int totalPoints,
        int currentLevel,
        int pointsUntilNextLevel
) {
}