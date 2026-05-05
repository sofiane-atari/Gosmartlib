package edu.ap.bib_backend.event;

import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.GamificationEventType;

public record ReviewCreatedGamificationEvent(
        String userId,
        Long reviewId,
        Long bookId,
        LocalDateTime occurredAt
) implements GamificationEvent {

    @Override
    public GamificationEventType type() {
        return GamificationEventType.REVIEW_CREATED;
    }
}