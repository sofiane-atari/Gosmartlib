package edu.ap.bib_backend.dto.loan;

public record LoanBookDTO(
        Long id,
        String title,
        String author,
        String isbn
) {}