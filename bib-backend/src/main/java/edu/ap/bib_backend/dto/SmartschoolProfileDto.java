package edu.ap.bib_backend.dto;

public record SmartschoolProfileDto(
    String userId,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String platform,
    String username
) {}