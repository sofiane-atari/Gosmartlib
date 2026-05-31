package edu.ap.bib_backend.service;

import java.util.List;

public record AudienceContext(List<String> classCodes, String yearCode, String schoolCode) {
    public static AudienceContext defaults() {
        return new AudienceContext(List.of("GENERAL"), "0", "ALLEMAAL");
    }

    public String primaryClassCode() {
        return classCodes.isEmpty() ? "GENERAL" : classCodes.get(0);
    }
}
