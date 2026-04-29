package edu.ap.bib_backend.dto.lessontips;

import edu.ap.bib_backend.jpa.enums.LessonTipType;
import edu.ap.bib_backend.jpa.enums.Role;

import java.time.LocalDateTime;
import java.util.List;

public record LessonTipResponseDto(
        Long id,
        String title,
        String description,
        LessonTipType lessonTipType,
        String subject,
        String educationLevel,
        String keywords,
        String createdByUserId,
        Role createdByRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<LessonTipBookSummaryResponseDto> linkedBooks
) {
}