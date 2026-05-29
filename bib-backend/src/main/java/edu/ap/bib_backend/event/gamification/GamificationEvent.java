package edu.ap.bib_backend.event.gamification;

import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.GamificationEventType;

public interface GamificationEvent {

    String userId();

    GamificationEventType type();

    LocalDateTime occurredAt();
}