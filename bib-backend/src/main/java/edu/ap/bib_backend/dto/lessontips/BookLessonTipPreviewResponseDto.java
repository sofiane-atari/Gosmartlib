package edu.ap.bib_backend.dto.lessontips;

import java.util.List;

public record BookLessonTipPreviewResponseDto(
        List<LessonTipPreviewResponseDto> lessonTips,
        long totalCount,
        boolean hasMore
) {
}