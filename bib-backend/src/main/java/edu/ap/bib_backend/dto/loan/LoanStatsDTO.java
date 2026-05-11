package edu.ap.bib_backend.dto.loan;

public record LoanStatsDTO(
        long activeLoans,
        long overdueLoans,
        long returnedLoans) {
}