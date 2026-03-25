package edu.ap.bib_backend.service;

public record AudienceContext(String classCode, String yearCode, String schoolCode) {
    public static AudienceContext defaults() {
        return new AudienceContext("GENERAL", "0", "ALLEMAAL");
    }
}

