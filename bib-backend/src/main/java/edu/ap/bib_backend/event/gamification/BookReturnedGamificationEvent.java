package edu.ap.bib_backend.event.gamification;

import java.time.LocalDate;
import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.GamificationEventType;

public record BookReturnedGamificationEvent(
        String targetUserId,
        Long loanId,
        boolean returnedOnTime,
        LocalDate dueDate,
        LocalDate returnedDate,
        LocalDateTime occurredAt
) implements GamificationEvent {

    @Override
    public String userId() {
        return targetUserId;
    }

    @Override
    public GamificationEventType type() {
        return returnedOnTime
                ? GamificationEventType.BOOK_RETURNED_ON_TIME
                : GamificationEventType.BOOK_RETURNED;
    }
}