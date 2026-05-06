package edu.ap.bib_backend.exception.gamification;

public class GamificationProfileNotFoundException extends RuntimeException {

    public GamificationProfileNotFoundException(String userId) {
        super("Gamification profile not found for user with id: " + userId);
    }
}