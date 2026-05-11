package edu.ap.bib_backend.dto;

public record SchoolSummaryDto(
    Long id,
    String name,
    Integer defaultLoanDurationDays
) {}
