package edu.ap.bib_backend.dto;

public record SmartschoolReportContext(
        String accessToken,
        String platform,
        String schoolCode) {
}