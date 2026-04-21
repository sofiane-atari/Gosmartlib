package edu.ap.bib_backend.dto.didactic;

import java.time.LocalDateTime;

public record DidacticMaterialAttachmentResponseDto(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}