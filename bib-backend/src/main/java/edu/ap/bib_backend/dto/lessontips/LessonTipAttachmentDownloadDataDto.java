package edu.ap.bib_backend.dto.lessontips;

import org.springframework.core.io.Resource;

public record LessonTipAttachmentDownloadDataDto(
        Resource resource,
        String originalFileName,
        String contentType
) {
}