package edu.ap.bib_backend.dto.loan;

import lombok.Builder;

@Builder
public record LoanBookDTO(
        Long id,
        String title,
        String author,
        String isbn,
        String imageUrl,
        Integer copiesCount
) {}
