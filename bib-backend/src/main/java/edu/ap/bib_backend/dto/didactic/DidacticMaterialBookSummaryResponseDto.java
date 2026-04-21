package edu.ap.bib_backend.dto.didactic;

public record DidacticMaterialBookSummaryResponseDto(
        Long id,
        String title,
        String author,
        String imageUrl

) {
}