package edu.ap.bib_backend.dto.lessontips;

import java.time.LocalDateTime;

public record LessonTipPreviewResponseDto(
        Long id,
        String title,
        String description,
        LocalDateTime updatedAt
) {
}