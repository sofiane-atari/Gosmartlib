package edu.ap.bib_backend.dto.lessontips;

public record LessonTipBookSummaryResponseDto(
        Long id,
        String title,
        String author,
        String imageUrl

) {
}