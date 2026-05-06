package edu.ap.bib_backend.exception.gamification;

public class BadgeNotFoundException extends RuntimeException {

    public BadgeNotFoundException(String badgeCode) {
        super("Badge not found with code: " + badgeCode);
    }
}