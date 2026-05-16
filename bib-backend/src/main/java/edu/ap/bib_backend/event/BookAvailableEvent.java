package edu.ap.bib_backend.event;

import java.time.LocalDateTime;

public record BookAvailableEvent(
    Long bookId,
    LocalDateTime occurredAt
) {
}
