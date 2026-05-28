package edu.ap.bib_backend.dto;

import edu.ap.bib_backend.jpa.enums.SchoolStatus;

public record SchoolSummaryDto(
    Long id,
    String name,
    String domainName,
    Integer defaultLoanDurationDays,
    SchoolStatus status
) {}
