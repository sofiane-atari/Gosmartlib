package edu.ap.bib_backend.dto;

public record LoanBookDTO(
        Long id,
        String title,
        String author,
        String isbn,
        Integer copiesCount
) {}
