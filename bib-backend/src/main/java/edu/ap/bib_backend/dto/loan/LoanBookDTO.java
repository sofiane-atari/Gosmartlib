package edu.ap.bib_backend.dto.loan;

import java.util.List;

import lombok.Builder;

@Builder
public record LoanBookDTO(
        Long id,
        String title,
        String author,
        String isbn,
        String imageUrl,
        List<Long> bookCopyIds,
        Integer copiesCount
) {}
