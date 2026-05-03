package edu.ap.bib_backend.dto.lessontips;

import java.time.LocalDateTime;

public record LessonTipAttachmentResponseDto(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}