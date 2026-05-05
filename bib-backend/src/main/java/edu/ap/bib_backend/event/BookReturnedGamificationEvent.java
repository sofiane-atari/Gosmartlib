package edu.ap.bib_backend.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.GamificationEventType;

public record BookReturnedGamificationEvent(
        String userId,
        Long loanId,
        boolean returnedOnTime,
        LocalDate dueDate,
        LocalDate returnedDate,
        LocalDateTime occurredAt
) implements GamificationEvent {

    @Override
    public GamificationEventType type() {
        if (returnedOnTime) {
            return GamificationEventType.BOOK_RETURNED_ON_TIME;
        }

        return GamificationEventType.BOOK_RETURNED;
    }
}